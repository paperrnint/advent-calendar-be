package com.example.adventcalendar.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.adventcalendar.dto.request.LetterCreateRequest;
import com.example.adventcalendar.dto.response.LetterResponse;
import com.example.adventcalendar.entity.Letter;
import com.example.adventcalendar.entity.User;
import com.example.adventcalendar.repository.LetterRepository;
import com.example.adventcalendar.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class LetterService {

	private final LetterRepository letterRepository;
	private final UserRepository userRepository;

	@Transactional
	public void createLetter(String uuid, LetterCreateRequest request) {
		User user = userRepository.findByShareUuid(uuid)
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다"));

		Letter letter = Letter.builder()
			.user(user)
			.day(request.getDay())
			.content(request.getContent())
			.fromName(request.getFromName())
			.build();

		letterRepository.save(letter);

		log.info("편지 작성 완료 - userId: {}, day: {}, from: {}", user.getId(), request.getDay(), request.getFromName());
	}

	@Transactional(readOnly = true)
	public List<LetterResponse> getLettersByUuid(String uuid, Long requestUserId) {
		User user = userRepository.findByShareUuid(uuid)
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다"));

		if (!user.getId().equals(requestUserId)) {
			throw new IllegalArgumentException("본인의 편지만 조회할 수 있습니다");
		}

		int currentDay = LocalDate.now(ZoneId.of("Asia/Seoul")).getDayOfMonth();

		/*LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
		if (today.getMonthValue() != 12) {
			log.warn("12월이 아닙니다 - 현재: {}", today);
			return List.of();
		}*/

		List<Letter> letters = letterRepository.findByUserIdAndDayLessThanEqual(user.getId(), currentDay);

		log.info("편지 조회 완료 - userId: {}, currentDay: {}, count: {}", user.getId(), currentDay, letters.size());

		return letters.stream()
			.map(LetterResponse::fromEntity)
			.collect(Collectors.toList());
	}
}
