package com.example.adventcalendar.controller;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.adventcalendar.dto.request.LetterCreateRequest;
import com.example.adventcalendar.dto.response.ApiResponse;
import com.example.adventcalendar.dto.response.LetterCountResponse;
import com.example.adventcalendar.dto.response.LetterResponse;
import com.example.adventcalendar.dto.response.UserInfoResponse;
import com.example.adventcalendar.entity.User;
import com.example.adventcalendar.exception.ResourceNotFoundException;
import com.example.adventcalendar.exception.UnauthorizedException;
import com.example.adventcalendar.repository.UserRepository;
import com.example.adventcalendar.service.LetterService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Tag(name = "편지", description = "어드벤트 캘린더 편지 API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LetterController {

	private final LetterService letterService;
	private final UserRepository userRepository;

	@Operation(summary = "유저 정보 조회", description = "UUID로 유저의 이름, 색상, UUID를 조회합니다")
	@GetMapping("/users/{uuid}")
	public ApiResponse<UserInfoResponse> getUserInfo(
		@Parameter(description = "유저 UUID") @PathVariable String uuid
	) {
		log.info("유저 정보 조회 요청 - uuid: {}", uuid);

		User user = userRepository.findByShareUuid(uuid)
			.orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 사용자입니다"));

		UserInfoResponse response = UserInfoResponse.fromEntity(user);

		return ApiResponse.success(response);
	}

	@Operation(summary = "편지 작성", description = "특정 유저에게 편지를 작성합니다 (비회원 가능)")
	@PostMapping("/{uuid}/letters")
	public ApiResponse<Void> createLetter(
		@Parameter(description = "받는 사람 UUID") @PathVariable String uuid,
		@Parameter(description = "편지 내용", required = true) @Valid @RequestBody LetterCreateRequest request
	) {
		log.info("편지 작성 요청 - uuid: {}, day: {}, from: {}", uuid, request.getDay(), request.getFromName());

		letterService.createLetter(uuid, request);

		return ApiResponse.success();
	}

	@Operation(summary = "편지 조회", description = "본인의 편지를 조회합니다 (본인만 가능, 현재 날짜 이하만)")
	@GetMapping("/{uuid}/letters")
	public ApiResponse<List<LetterResponse>> getLetters(
		@Parameter(description = "유저 UUID") @PathVariable String uuid,
		@Parameter(description = "현재 로그인된 사용자 ID", hidden = true) Authentication authentication
	) {
		if (authentication == null || authentication.getPrincipal() == null) {
			throw new UnauthorizedException("인증이 필요합니다");
		}

		Long userId = (Long) authentication.getPrincipal();
		log.info("편지 조회 요청 - uuid: {}, userId: {}", uuid, userId);

		List<LetterResponse> letters = letterService.getLettersByUuid(uuid, userId);

		return ApiResponse.success(letters);
	}

	@Operation(summary = "특정 날짜 편지 조회", description = "본인의 특정 날짜 편지를 조회합니다 (본인만 가능, 해당 날짜 또는 이전 날짜만)")
	@GetMapping("/{uuid}/letters/{day}")
	public ApiResponse<List<LetterResponse>> getLettersByDay(
		@Parameter(description = "유저 UUID") @PathVariable String uuid,
		@Parameter(description = "조회할 날짜 (1-25)") @PathVariable Integer day,
		@Parameter(description = "현재 로그인된 사용자 ID", hidden = true) Authentication authentication
	) {
		if (authentication == null || authentication.getPrincipal() == null) {
			throw new UnauthorizedException("인증이 필요합니다");
		}

		Long userId = (Long) authentication.getPrincipal();
		log.info("특정 날짜 편지 조회 요청 - uuid: {}, day: {}, userId: {}", uuid, day, userId);

		List<LetterResponse> letters = letterService.getLettersByDay(uuid, day, userId);

		return ApiResponse.success(letters);
	}

	@Operation(summary = "날짜별 편지 개수 조회", description = "모든 날짜(1-25일)의 편지 개수를 조회합니다 (인증 불필요)")
	@GetMapping("/{uuid}/letters/count")
	public ApiResponse<LetterCountResponse> getLetterCounts(
		@Parameter(description = "유저 UUID") @PathVariable String uuid
	) {
		log.info("날짜별 편지 개수 조회 요청 - uuid: {}", uuid);

		Map<Integer, Long> counts = letterService.getLetterCountsByUuid(uuid);
		LetterCountResponse response = LetterCountResponse.create(counts);

		return ApiResponse.success(response);
	}
}
