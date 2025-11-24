package com.example.adventcalendar.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.adventcalendar.constant.UserStatus;
import com.example.adventcalendar.dto.request.LetterCreateRequest;
import com.example.adventcalendar.dto.response.LetterResponse;
import com.example.adventcalendar.entity.Letter;
import com.example.adventcalendar.entity.User;
import com.example.adventcalendar.exception.ForbiddenException;
import com.example.adventcalendar.exception.ResourceNotFoundException;
import com.example.adventcalendar.repository.LetterRepository;
import com.example.adventcalendar.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("LetterService 단위 테스트")
class LetterServiceTest {

	@Mock
	private LetterRepository letterRepository;

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private LetterService letterService;

	private User user;
	private Letter letter;

	@BeforeEach
	void setUp() {
		user = User.builder()
			.id(1L)
			.email("test@example.com")
			.name("테스트")
			.oauthProvider("NAVER")
			.oauthId("naver123")
			.selectedColor("green")
			.shareUuid("test-uuid-123")
			.status(UserStatus.ACTIVE)
			.build();

		letter = Letter.builder()
			.id(1L)
			.user(user)
			.day(10)
			.content("메리크리스마스!")
			.fromName("산타")
			.build();
	}

	@Nested
	@DisplayName("편지 작성")
	class CreateLetter {

		@Test
		@DisplayName("정상적으로 편지 작성")
		void createLetter_Success() {
			// given
			String uuid = "test-uuid-123";
			LetterCreateRequest request = new LetterCreateRequest(10, "메리크리스마스!", "산타");

			given(userRepository.findByShareUuid(uuid)).willReturn(Optional.of(user));
			given(letterRepository.save(any(Letter.class))).willReturn(letter);

			// when
			letterService.createLetter(uuid, request);

			// then
			verify(letterRepository).save(argThat(savedLetter ->
				savedLetter.getUser().getId().equals(1L) &&
					savedLetter.getDay().equals(10) &&
					savedLetter.getContent().equals("메리크리스마스!") &&
					savedLetter.getFromName().equals("산타")
			));
		}

		@Test
		@DisplayName("존재하지 않는 UUID로 편지 작성 시 예외 발생")
		void createLetter_UserNotFound_ThrowsException() {
			// given
			String uuid = "invalid-uuid";
			LetterCreateRequest request = new LetterCreateRequest(10, "내용", "보내는사람");

			given(userRepository.findByShareUuid(uuid)).willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> letterService.createLetter(uuid, request))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("존재하지 않는 사용자입니다");

			verify(letterRepository, never()).save(any());
		}

		@Test
		@DisplayName("XSS 방어 - HTML 이스케이프 적용")
		void createLetter_XssDefense_EscapesHtml() {
			// given
			String uuid = "test-uuid-123";
			LetterCreateRequest request = new LetterCreateRequest(
				10,
				"<script>alert('xss')</script>",
				"<img src=x onerror=alert('xss')>"
			);

			given(userRepository.findByShareUuid(uuid)).willReturn(Optional.of(user));
			given(letterRepository.save(any(Letter.class))).willReturn(letter);

			// when
			letterService.createLetter(uuid, request);

			// then
			verify(letterRepository).save(argThat(savedLetter ->
				!savedLetter.getContent().contains("<script>") &&
					savedLetter.getContent().contains("&lt;") &&
					!savedLetter.getFromName().contains("<img") &&
					savedLetter.getFromName().contains("&lt;")
			));
		}
	}

	@Nested
	@DisplayName("편지 전체 조회")
	class GetLettersByUuid {

		@BeforeEach
		void setUp() {
			// 날짜 검증 활성화
			ReflectionTestUtils.setField(letterService, "validateDate", true);
			ReflectionTestUtils.setField(letterService, "validateMonth", true);
		}

		@Test
		@DisplayName("본인의 편지 조회 성공 (12월, 날짜 검증 활성화)")
		void getLettersByUuid_OwnLetters_Success() {
			// given
			String uuid = "test-uuid-123";
			Long requestUserId = 1L;

			LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
			int currentDay = today.getDayOfMonth();
			int currentMonth = today.getMonthValue();

			// 12월이 아니면 테스트 스킵
			if (currentMonth != 12) {
				return;
			}

			Letter letter1 = Letter.builder()
				.id(1L)
				.user(user)
				.day(1)
				.content("첫 번째 편지")
				.fromName("친구1")
				.build();

			Letter letter2 = Letter.builder()
				.id(2L)
				.user(user)
				.day(currentDay)
				.content("오늘의 편지")
				.fromName("친구2")
				.build();

			List<Letter> letters = Arrays.asList(letter1, letter2);

			given(userRepository.findByShareUuid(uuid)).willReturn(Optional.of(user));
			given(letterRepository.findByUserIdAndDayLessThanEqual(1L, currentDay))
				.willReturn(letters);

			// when
			List<LetterResponse> responses = letterService.getLettersByUuid(uuid, requestUserId);

			// then
			assertThat(responses).hasSize(2);
			assertThat(responses.get(0).getDay()).isEqualTo(1);
			assertThat(responses.get(0).getContent()).isEqualTo("첫 번째 편지");
			assertThat(responses.get(1).getDay()).isEqualTo(currentDay);
		}

		@Test
		@DisplayName("12월이 아닐 때는 빈 리스트 반환 (월 검증 활성화)")
		void getLettersByUuid_NotDecember_ReturnsEmptyList() {
			// given
			String uuid = "test-uuid-123";
			Long requestUserId = 1L;

			LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

			// 12월이면 테스트 스킵
			if (today.getMonthValue() == 12) {
				return;
			}

			given(userRepository.findByShareUuid(uuid)).willReturn(Optional.of(user));

			// when
			List<LetterResponse> responses = letterService.getLettersByUuid(uuid, requestUserId);

			// then
			assertThat(responses).isEmpty();
			verify(letterRepository, never()).findByUserIdAndDayLessThanEqual(anyLong(), anyInt());
		}

		@Test
		@DisplayName("날짜 검증 비활성화 시 모든 편지 조회")
		void getLettersByUuid_ValidateDateDisabled_ReturnsAllLetters() {
			// given
			ReflectionTestUtils.setField(letterService, "validateDate", false);
			ReflectionTestUtils.setField(letterService, "validateMonth", false);

			String uuid = "test-uuid-123";
			Long requestUserId = 1L;

			Letter letter1 = Letter.builder().id(1L).user(user).day(1).content("편지1").fromName("A").build();
			Letter letter2 = Letter.builder().id(2L).user(user).day(25).content("편지25").fromName("B").build();

			List<Letter> letters = Arrays.asList(letter1, letter2);

			given(userRepository.findByShareUuid(uuid)).willReturn(Optional.of(user));
			given(letterRepository.findByUserId(1L)).willReturn(letters);

			// when
			List<LetterResponse> responses = letterService.getLettersByUuid(uuid, requestUserId);

			// then
			assertThat(responses).hasSize(2);
			assertThat(responses.get(0).getDay()).isEqualTo(1);
			assertThat(responses.get(1).getDay()).isEqualTo(25);

			verify(letterRepository).findByUserId(1L);
			verify(letterRepository, never()).findByUserIdAndDayLessThanEqual(anyLong(), anyInt());
		}

		@Test
		@DisplayName("다른 사용자의 편지 조회 시 예외 발생")
		void getLettersByUuid_OtherUserLetters_ThrowsException() {
			// given
			String uuid = "test-uuid-123";
			Long requestUserId = 999L; // 다른 사용자

			given(userRepository.findByShareUuid(uuid)).willReturn(Optional.of(user));

			// when & then
			assertThatThrownBy(() -> letterService.getLettersByUuid(uuid, requestUserId))
				.isInstanceOf(ForbiddenException.class)
				.hasMessage("본인의 편지만 조회할 수 있습니다");

			verify(letterRepository, never()).findByUserIdAndDayLessThanEqual(anyLong(), anyInt());
		}

		@Test
		@DisplayName("존재하지 않는 UUID 예외 발생")
		void getLettersByUuid_UserNotFound_ThrowsException() {
			// given
			String uuid = "invalid-uuid";
			Long requestUserId = 1L;

			given(userRepository.findByShareUuid(uuid)).willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> letterService.getLettersByUuid(uuid, requestUserId))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("존재하지 않는 사용자입니다");
		}
	}

	@Nested
	@DisplayName("특정 날짜 편지 조회")
	class GetLettersByDay {

		@BeforeEach
		void setUp() {
			ReflectionTestUtils.setField(letterService, "validateDate", true);
			ReflectionTestUtils.setField(letterService, "validateMonth", true);
		}

		@Test
		@DisplayName("현재 날짜의 편지 조회 성공 (12월)")
		void getLettersByDay_CurrentDay_Success() {
			// given
			String uuid = "test-uuid-123";
			Long requestUserId = 1L;

			LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
			int currentDay = today.getDayOfMonth();
			int currentMonth = today.getMonthValue();

			// 12월이 아니면 테스트 스킵
			if (currentMonth != 12) {
				return;
			}

			Letter todayLetter = Letter.builder()
				.id(1L)
				.user(user)
				.day(currentDay)
				.content("오늘의 편지")
				.fromName("친구")
				.build();

			given(userRepository.findByShareUuid(uuid)).willReturn(Optional.of(user));
			given(letterRepository.findByUserIdAndDay(1L, currentDay))
				.willReturn(List.of(todayLetter));

			// when
			List<LetterResponse> responses = letterService.getLettersByDay(uuid, currentDay, requestUserId);

			// then
			assertThat(responses).hasSize(1);
			assertThat(responses.get(0).getDay()).isEqualTo(currentDay);
			assertThat(responses.get(0).getContent()).isEqualTo("오늘의 편지");
		}

		@Test
		@DisplayName("과거 날짜의 편지 조회 성공 (12월)")
		void getLettersByDay_PastDay_Success() {
			// given
			String uuid = "test-uuid-123";
			Long requestUserId = 1L;
			Integer pastDay = 1;

			LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

			// 12월이 아니면 테스트 스킵
			if (today.getMonthValue() != 12) {
				return;
			}

			Letter pastLetter = Letter.builder()
				.id(1L)
				.user(user)
				.day(pastDay)
				.content("과거 편지")
				.fromName("친구")
				.build();

			given(userRepository.findByShareUuid(uuid)).willReturn(Optional.of(user));
			given(letterRepository.findByUserIdAndDay(1L, pastDay))
				.willReturn(List.of(pastLetter));

			// when
			List<LetterResponse> responses = letterService.getLettersByDay(uuid, pastDay, requestUserId);

			// then
			assertThat(responses).hasSize(1);
			assertThat(responses.get(0).getDay()).isEqualTo(pastDay);
		}

		@Test
		@DisplayName("미래 날짜 조회 시 예외 발생 (12월, 날짜 검증 활성화)")
		void getLettersByDay_FutureDay_ThrowsException() {
			// given
			String uuid = "test-uuid-123";
			Long requestUserId = 1L;

			LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
			int currentDay = today.getDayOfMonth();
			int currentMonth = today.getMonthValue();

			// 12월이 아니거나 25일이면 테스트 스킵
			if (currentMonth != 12 || currentDay >= 25) {
				return;
			}

			Integer futureDay = currentDay + 1;

			given(userRepository.findByShareUuid(uuid)).willReturn(Optional.of(user));

			// when & then
			assertThatThrownBy(() -> letterService.getLettersByDay(uuid, futureDay, requestUserId))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("미래 날짜의 편지는 아직 열어볼 수 없습니다");

			verify(letterRepository, never()).findByUserIdAndDay(anyLong(), anyInt());
		}

		@Test
		@DisplayName("12월이 아닐 때 예외 발생 (월 검증 활성화)")
		void getLettersByDay_NotDecember_ThrowsException() {
			// given
			String uuid = "test-uuid-123";
			Long requestUserId = 1L;
			Integer day = 10;

			LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

			// 12월이면 테스트 스킵
			if (today.getMonthValue() == 12) {
				return;
			}

			given(userRepository.findByShareUuid(uuid)).willReturn(Optional.of(user));

			// when & then
			assertThatThrownBy(() -> letterService.getLettersByDay(uuid, day, requestUserId))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("어드벤트 캘린더는 12월에만 이용 가능합니다");
		}

		@Test
		@DisplayName("날짜 검증 비활성화 시 미래 날짜도 조회 가능")
		void getLettersByDay_ValidateDateDisabled_AllowsFutureDay() {
			// given
			ReflectionTestUtils.setField(letterService, "validateDate", false);
			ReflectionTestUtils.setField(letterService, "validateMonth", false);

			String uuid = "test-uuid-123";
			Long requestUserId = 1L;
			Integer futureDay = 25;

			Letter futureLetter = Letter.builder()
				.id(1L)
				.user(user)
				.day(futureDay)
				.content("크리스마스!")
				.fromName("산타")
				.build();

			given(userRepository.findByShareUuid(uuid)).willReturn(Optional.of(user));
			given(letterRepository.findByUserIdAndDay(1L, futureDay))
				.willReturn(List.of(futureLetter));

			// when
			List<LetterResponse> responses = letterService.getLettersByDay(uuid, futureDay, requestUserId);

			// then
			assertThat(responses).hasSize(1);
			assertThat(responses.get(0).getDay()).isEqualTo(futureDay);
			assertThat(responses.get(0).getContent()).isEqualTo("크리스마스!");
		}

		@Test
		@DisplayName("잘못된 날짜 범위 (1-25 외) 예외 발생")
		void getLettersByDay_InvalidDayRange_ThrowsException() {
			// given
			String uuid = "test-uuid-123";
			Long requestUserId = 1L;

			given(userRepository.findByShareUuid(uuid)).willReturn(Optional.of(user));

			// when & then - day < 1
			assertThatThrownBy(() -> letterService.getLettersByDay(uuid, 0, requestUserId))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("날짜는 1일부터 25일까지입니다");

			// when & then - day > 25
			assertThatThrownBy(() -> letterService.getLettersByDay(uuid, 26, requestUserId))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("날짜는 1일부터 25일까지입니다");
		}

		@Test
		@DisplayName("다른 사용자의 편지 조회 시 예외 발생")
		void getLettersByDay_OtherUser_ThrowsException() {
			// given
			String uuid = "test-uuid-123";
			Long requestUserId = 999L; // 다른 사용자
			Integer day = 10;

			given(userRepository.findByShareUuid(uuid)).willReturn(Optional.of(user));

			// when & then
			assertThatThrownBy(() -> letterService.getLettersByDay(uuid, day, requestUserId))
				.isInstanceOf(ForbiddenException.class)
				.hasMessage("본인의 편지만 조회할 수 있습니다");
		}
	}

	@Nested
	@DisplayName("날짜별 편지 개수 조회")
	class GetLetterCountsByUuid {

		@Test
		@DisplayName("날짜별 편지 개수 조회 성공")
		void getLetterCountsByUuid_Success() {
			// given
			String uuid = "test-uuid-123";

			List<Object[]> dbResults = Arrays.asList(
				new Object[]{1, 3L},
				new Object[]{5, 2L},
				new Object[]{10, 5L},
				new Object[]{25, 1L}
			);

			given(userRepository.findByShareUuid(uuid)).willReturn(Optional.of(user));
			given(letterRepository.countByUserIdGroupByDay(1L)).willReturn(dbResults);

			// when
			Map<Integer, Long> counts = letterService.getLetterCountsByUuid(uuid);

			// then
			assertThat(counts).isNotNull();
			assertThat(counts.get(1)).isEqualTo(3L);
			assertThat(counts.get(5)).isEqualTo(2L);
			assertThat(counts.get(10)).isEqualTo(5L);
			assertThat(counts.get(25)).isEqualTo(1L);
			assertThat(counts.get(2)).isNull(); // 편지가 없는 날짜
		}

		@Test
		@DisplayName("편지가 없는 경우 빈 Map 반환")
		void getLetterCountsByUuid_NoLetters_ReturnsEmptyMap() {
			// given
			String uuid = "test-uuid-123";

			given(userRepository.findByShareUuid(uuid)).willReturn(Optional.of(user));
			given(letterRepository.countByUserIdGroupByDay(1L)).willReturn(List.of());

			// when
			Map<Integer, Long> counts = letterService.getLetterCountsByUuid(uuid);

			// then
			assertThat(counts).isEmpty();
		}

		@Test
		@DisplayName("존재하지 않는 UUID 예외 발생")
		void getLetterCountsByUuid_UserNotFound_ThrowsException() {
			// given
			String uuid = "invalid-uuid";

			given(userRepository.findByShareUuid(uuid)).willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> letterService.getLetterCountsByUuid(uuid))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("존재하지 않는 사용자입니다");
		}

		@Test
		@DisplayName("모든 날짜에 편지가 있는 경우")
		void getLetterCountsByUuid_AllDaysHaveLetters_Success() {
			// given
			String uuid = "test-uuid-123";

			List<Object[]> dbResults = new java.util.ArrayList<>();
			for (int day = 1; day <= 25; day++) {
				dbResults.add(new Object[]{day, (long) day});
			}

			given(userRepository.findByShareUuid(uuid)).willReturn(Optional.of(user));
			given(letterRepository.countByUserIdGroupByDay(1L)).willReturn(dbResults);

			// when
			Map<Integer, Long> counts = letterService.getLetterCountsByUuid(uuid);

			// then
			assertThat(counts).hasSize(25);
			for (int day = 1; day <= 25; day++) {
				assertThat(counts.get(day)).isEqualTo((long) day);
			}
		}
	}
}
