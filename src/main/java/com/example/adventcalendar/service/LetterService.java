package com.example.adventcalendar.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.adventcalendar.dto.request.LetterCreateRequest;
import com.example.adventcalendar.dto.response.LetterResponse;
import com.example.adventcalendar.entity.Letter;
import com.example.adventcalendar.entity.User;
import com.example.adventcalendar.exception.ForbiddenException;
import com.example.adventcalendar.exception.ResourceNotFoundException;
import com.example.adventcalendar.repository.LetterRepository;
import com.example.adventcalendar.repository.UserRepository;
import com.example.adventcalendar.util.XssUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class LetterService {

	private final LetterRepository letterRepository;
	private final UserRepository userRepository;

	@Value("${app.advent.validate-date:true}")
	private boolean validateDate;

	@Value("${app.advent.validate-month:true}")
	private boolean validateMonth;

	@Transactional
	public void createLetter(String uuid, LetterCreateRequest request) {
		User user = userRepository.findByShareUuid(uuid)
			.orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 사용자입니다"));

		String sanitizedContent = XssUtils.sanitizeHtml(request.getContent());
		String sanitizedFromName = XssUtils.sanitizeHtml(request.getFromName());

		Letter letter = Letter.builder()
			.user(user)
			.day(request.getDay())
			.content(sanitizedContent)
			.fromName(sanitizedFromName)
			.build();

		letterRepository.save(letter);

		log.info("편지 작성 완료 - userId: {}, day: {}, from: {}", user.getId(), request.getDay(), sanitizedFromName);
	}

	@Transactional(readOnly = true)
	public List<LetterResponse> getLettersByUuid(String uuid, Long requestUserId) {
		User user = userRepository.findByShareUuid(uuid)
			.orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 사용자입니다"));

		if (!user.getId().equals(requestUserId)) {
			throw new ForbiddenException("본인의 편지만 조회할 수 있습니다");
		}

		int currentDay = LocalDate.now(ZoneId.of("Asia/Seoul")).getDayOfMonth();

		// 12월 확인
		if (validateMonth) {
			LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
			if (today.getMonthValue() != 12) {
				log.warn("12월이 아닙니다 - 현재: {}", today);
				return List.of();
			}
		}

		// 현재 날짜 이하의 편지만 조회
		List<Letter> letters;
		if (validateDate) {
			letters = letterRepository.findByUserIdAndDayLessThanEqual(user.getId(), currentDay);
		} else {
			letters = letterRepository.findByUserId(user.getId());
		}

		log.info("편지 조회 완료 - userId: {}, currentDay: {}, count: {}", user.getId(), currentDay, letters.size());

		return letters.stream()
			.map(LetterResponse::fromEntity)
			.collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public List<LetterResponse> getLettersByDay(String uuid, Integer day, Long requestUserId) {
		User user = userRepository.findByShareUuid(uuid)
			.orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 사용자입니다"));

		if (!user.getId().equals(requestUserId)) {
			throw new ForbiddenException("본인의 편지만 조회할 수 있습니다");
		}

		if (day < 1 || day > 25) {
			throw new IllegalArgumentException("날짜는 1일부터 25일까지입니다");
		}

		int currentDay = LocalDate.now(ZoneId.of("Asia/Seoul")).getDayOfMonth();

		// 12월 확인
		if (validateMonth) {
			LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
			if (today.getMonthValue() != 12) {
				log.warn("12월이 아닙니다 - 현재: {}", today);
				throw new IllegalArgumentException("어드벤트 캘린더는 12월에만 이용 가능합니다");
			}
		}

		// 현재 날짜 이하의 편지만 조회
		if (validateDate && day > currentDay) {
			log.warn("미래 날짜의 편지는 조회할 수 없습니다 - 요청 day: {}, 현재 day: {}", day, currentDay);
			throw new IllegalArgumentException("미래 날짜의 편지는 아직 열어볼 수 없습니다");
		}

		List<Letter> letters = letterRepository.findByUserIdAndDay(user.getId(), day);

		log.info("특정 날짜 편지 조회 완료 - userId: {}, day: {}, count: {}", user.getId(), day, letters.size());

		return letters.stream()
			.map(LetterResponse::fromEntity)
			.collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public Map<Integer, Long> getLetterCountsByUuid(String uuid) {
		User user = userRepository.findByShareUuid(uuid)
			.orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 사용자입니다"));

		List<Object[]> results = letterRepository.countByUserIdGroupByDay(user.getId());

		Map<Integer, Long> counts = new HashMap<>();
		for (Object[] result : results) {
			Integer day = (Integer) result[0];
			Long count = (Long) result[1];
			counts.put(day, count);
		}

		log.info("날짜별 편지 개수 조회 완료 - userId: {}, totalDays: {}", user.getId(), counts.size());

		return counts;
	}
}
