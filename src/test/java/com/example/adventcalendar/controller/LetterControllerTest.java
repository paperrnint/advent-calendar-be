package com.example.adventcalendar.controller;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.example.adventcalendar.config.JwtTokenProvider;
import com.example.adventcalendar.constant.UserStatus;
import com.example.adventcalendar.dto.request.LetterCreateRequest;
import com.example.adventcalendar.entity.Letter;
import com.example.adventcalendar.entity.User;
import com.example.adventcalendar.repository.LetterRepository;
import com.example.adventcalendar.repository.UserRepository;
import com.example.adventcalendar.service.LetterService;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.Cookie;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("LetterController 통합 테스트")
class LetterControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private LetterRepository letterRepository;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@Autowired
	private LetterService letterService;

	private User user;
	private User anotherUser;
	private String accessToken;

	@BeforeEach
	void setUp() {
		letterRepository.deleteAll();
		userRepository.deleteAll();

		// 날짜 검증 비활성화
		ReflectionTestUtils.setField(letterService, "validateDate", false);
		ReflectionTestUtils.setField(letterService, "validateMonth", false);

		user = User.builder()
			.email("user@example.com")
			.name("사용자")
			.oauthProvider("NAVER")
			.oauthId("naver123")
			.selectedColor("green")
			.shareUuid("user-uuid-123")
			.status(UserStatus.ACTIVE)
			.build();
		user = userRepository.save(user);

		anotherUser = User.builder()
			.email("another@example.com")
			.name("다른사용자")
			.oauthProvider("KAKAO")
			.oauthId("kakao456")
			.selectedColor("blue")
			.shareUuid("another-uuid-456")
			.status(UserStatus.ACTIVE)
			.build();
		anotherUser = userRepository.save(anotherUser);

		accessToken = jwtTokenProvider.createAccessToken(
			user.getId(),
			user.getEmail(),
			user.getOauthProvider()
		);
	}

	@Nested
	@DisplayName("GET /api/users/{uuid} - 유저 정보 조회")
	class GetUserInfo {

		@Test
		@DisplayName("UUID로 유저 정보 조회 성공")
		void getUserInfo_Success() throws Exception {
			// when & then
			mockMvc.perform(get("/api/users/{uuid}", user.getShareUuid()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.data.name").value("사용자"))
				.andExpect(jsonPath("$.data.color").value("green"))
				.andExpect(jsonPath("$.data.uuid").value("user-uuid-123"));
		}

		@Test
		@DisplayName("존재하지 않는 UUID는 404 에러")
		void getUserInfo_NotFound_Returns404() throws Exception {
			// when & then
			mockMvc.perform(get("/api/users/{uuid}", "invalid-uuid"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.message").value("존재하지 않는 사용자입니다"))
				.andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
		}
	}

	@Nested
	@DisplayName("POST /api/{uuid}/letters - 편지 작성")
	class CreateLetter {

		@Test
		@DisplayName("비회원이 편지 작성 성공")
		void createLetter_NonMember_Success() throws Exception {
			// given
			LetterCreateRequest request = new LetterCreateRequest(10, "메리크리스마스!", "산타");

			// when & then
			mockMvc.perform(post("/api/{uuid}/letters", user.getShareUuid())
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200));

			// DB 검증
			List<Letter> letters = letterRepository.findByUserId(user.getId());
			assertThat(letters).hasSize(1);
			assertThat(letters.get(0).getDay()).isEqualTo(10);
			assertThat(letters.get(0).getContent()).isEqualTo("메리크리스마스!");
			assertThat(letters.get(0).getFromName()).isEqualTo("산타");
		}

		@Test
		@DisplayName("존재하지 않는 UUID는 404 에러")
		void createLetter_InvalidUuid_Returns404() throws Exception {
			// given
			LetterCreateRequest request = new LetterCreateRequest(10, "내용", "보내는사람");

			// when & then
			mockMvc.perform(post("/api/{uuid}/letters", "invalid-uuid")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.message").value("존재하지 않는 사용자입니다"));
		}

		@Test
		@DisplayName("날짜 validation 실패 - 범위 초과")
		void createLetter_InvalidDay_ValidationFailed() throws Exception {
			// given
			LetterCreateRequest request = new LetterCreateRequest(26, "내용", "보내는사람");

			// when & then
			mockMvc.perform(post("/api/{uuid}/letters", user.getShareUuid())
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
		}

		@Test
		@DisplayName("내용 validation 실패 - 빈 값")
		void createLetter_EmptyContent_ValidationFailed() throws Exception {
			// given
			LetterCreateRequest request = new LetterCreateRequest(10, "", "보내는사람");

			// when & then
			mockMvc.perform(post("/api/{uuid}/letters", user.getShareUuid())
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
		}

		@Test
		@DisplayName("보내는사람 validation 실패 - 빈 값")
		void createLetter_EmptyFromName_ValidationFailed() throws Exception {
			// given
			LetterCreateRequest request = new LetterCreateRequest(10, "내용", "");

			// when & then
			mockMvc.perform(post("/api/{uuid}/letters", user.getShareUuid())
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
		}

		@Test
		@DisplayName("XSS 방어 - HTML 태그 이스케이프")
		void createLetter_XssDefense_EscapesHtml() throws Exception {
			// given
			LetterCreateRequest request = new LetterCreateRequest(
				10,
				"<script>alert('xss')</script>안녕하세요",
				"<img src=x onerror=alert(1)>"
			);

			// when
			mockMvc.perform(post("/api/{uuid}/letters", user.getShareUuid())
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk());

			// then
			List<Letter> letters = letterRepository.findByUserId(user.getId());
			assertThat(letters).hasSize(1);
			assertThat(letters.get(0).getContent()).doesNotContain("<script>");
			assertThat(letters.get(0).getContent()).contains("&lt;script&gt;");
			assertThat(letters.get(0).getFromName()).doesNotContain("<img");
			assertThat(letters.get(0).getFromName()).contains("&lt;img");
		}

		@Test
		@DisplayName("같은 날짜에 여러 편지 작성 가능")
		void createLetter_MultipleSameDay_Success() throws Exception {
			// given
			LetterCreateRequest request1 = new LetterCreateRequest(10, "첫 번째 편지", "친구1");
			LetterCreateRequest request2 = new LetterCreateRequest(10, "두 번째 편지", "친구2");

			// when
			mockMvc.perform(post("/api/{uuid}/letters", user.getShareUuid())
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request1)))
				.andExpect(status().isOk());

			mockMvc.perform(post("/api/{uuid}/letters", user.getShareUuid())
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request2)))
				.andExpect(status().isOk());

			// then
			List<Letter> letters = letterRepository.findByUserIdAndDay(user.getId(), 10);
			assertThat(letters).hasSize(2);
		}
	}

	@Nested
	@DisplayName("GET /api/{uuid}/letters - 편지 전체 조회")
	class GetLetters {

		@BeforeEach
		void setUpLetters() {
			Letter letter1 = Letter.builder()
				.user(user)
				.day(1)
				.content("첫 번째 편지")
				.fromName("친구1")
				.build();

			Letter letter2 = Letter.builder()
				.user(user)
				.day(10)
				.content("열 번째 편지")
				.fromName("친구2")
				.build();

			Letter letter3 = Letter.builder()
				.user(user)
				.day(25)
				.content("크리스마스 편지")
				.fromName("산타")
				.build();

			letterRepository.save(letter1);
			letterRepository.save(letter2);
			letterRepository.save(letter3);
		}

		@Test
		@DisplayName("본인의 편지 조회 성공 (날짜 검증 비활성화)")
		void getLetters_OwnLetters_Success() throws Exception {
			// when & then
			mockMvc.perform(get("/api/{uuid}/letters", user.getShareUuid())
					.cookie(new Cookie("accessToken", accessToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.data").isArray())
				.andExpect(jsonPath("$.data.length()").value(3))
				.andExpect(jsonPath("$.data[0].day").exists())
				.andExpect(jsonPath("$.data[0].content").exists())
				.andExpect(jsonPath("$.data[0].fromName").exists())
				.andExpect(jsonPath("$.data[0].createdAt").exists());
		}

		@Test
		@DisplayName("다른 사용자의 편지 조회 시 403 에러")
		void getLetters_OtherUserLetters_Returns403() throws Exception {
			// given
			String anotherAccessToken = jwtTokenProvider.createAccessToken(
				anotherUser.getId(),
				anotherUser.getEmail(),
				anotherUser.getOauthProvider()
			);

			// when & then
			mockMvc.perform(get("/api/{uuid}/letters", user.getShareUuid())
					.cookie(new Cookie("accessToken", anotherAccessToken)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.status").value(403))
				.andExpect(jsonPath("$.message").value("본인의 편지만 조회할 수 있습니다"))
				.andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
		}

		@Test
		@DisplayName("인증되지 않은 요청은 401 에러")
		void getLetters_Unauthorized_Returns401() throws Exception {
			// when & then
			mockMvc.perform(get("/api/{uuid}/letters", user.getShareUuid()))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.message").value("인증이 필요합니다"));
		}

		@Test
		@DisplayName("존재하지 않는 UUID는 404 에러")
		void getLetters_InvalidUuid_Returns404() throws Exception {
			// when & then
			mockMvc.perform(get("/api/{uuid}/letters", "invalid-uuid")
					.cookie(new Cookie("accessToken", accessToken)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.message").value("존재하지 않는 사용자입니다"));
		}

		@Test
		@DisplayName("편지가 없는 경우 빈 배열 반환")
		void getLetters_NoLetters_ReturnsEmptyArray() throws Exception {
			// given
			letterRepository.deleteAll();

			// when & then
			mockMvc.perform(get("/api/{uuid}/letters", user.getShareUuid())
					.cookie(new Cookie("accessToken", accessToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.data").isArray())
				.andExpect(jsonPath("$.data.length()").value(0));
		}
	}

	@Nested
	@DisplayName("GET /api/{uuid}/letters/{day} - 특정 날짜 편지 조회")
	class GetLettersByDay {

		@BeforeEach
		void setUpLetters() {
			Letter letter1 = Letter.builder()
				.user(user)
				.day(10)
				.content("첫 번째 편지")
				.fromName("친구1")
				.build();

			Letter letter2 = Letter.builder()
				.user(user)
				.day(10)
				.content("두 번째 편지")
				.fromName("친구2")
				.build();

			Letter letter3 = Letter.builder()
				.user(user)
				.day(15)
				.content("15일 편지")
				.fromName("친구3")
				.build();

			letterRepository.save(letter1);
			letterRepository.save(letter2);
			letterRepository.save(letter3);
		}

		@Test
		@DisplayName("특정 날짜 편지 조회 성공 (날짜 검증 비활성화)")
		void getLettersByDay_Success() throws Exception {
			// when & then
			mockMvc.perform(get("/api/{uuid}/letters/{day}", user.getShareUuid(), 10)
					.cookie(new Cookie("accessToken", accessToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.data").isArray())
				.andExpect(jsonPath("$.data.length()").value(2))
				.andExpect(jsonPath("$.data[0].day").value(10))
				.andExpect(jsonPath("$.data[1].day").value(10));
		}

		@Test
		@DisplayName("해당 날짜에 편지가 없으면 빈 배열")
		void getLettersByDay_NoLetters_ReturnsEmptyArray() throws Exception {
			// when & then
			mockMvc.perform(get("/api/{uuid}/letters/{day}", user.getShareUuid(), 5)
					.cookie(new Cookie("accessToken", accessToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.data").isArray())
				.andExpect(jsonPath("$.data.length()").value(0));
		}

		@Test
		@DisplayName("다른 사용자의 편지 조회 시 403 에러")
		void getLettersByDay_OtherUser_Returns403() throws Exception {
			// given
			String anotherAccessToken = jwtTokenProvider.createAccessToken(
				anotherUser.getId(),
				anotherUser.getEmail(),
				anotherUser.getOauthProvider()
			);

			// when & then
			mockMvc.perform(get("/api/{uuid}/letters/{day}", user.getShareUuid(), 10)
					.cookie(new Cookie("accessToken", anotherAccessToken)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.status").value(403))
				.andExpect(jsonPath("$.message").value("본인의 편지만 조회할 수 있습니다"));
		}

		@Test
		@DisplayName("인증되지 않은 요청은 401 에러")
		void getLettersByDay_Unauthorized_Returns401() throws Exception {
			// when & then
			mockMvc.perform(get("/api/{uuid}/letters/{day}", user.getShareUuid(), 10))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401));
		}

		@Test
		@DisplayName("잘못된 날짜 범위 - 0일")
		void getLettersByDay_InvalidDay_Zero_Returns400() throws Exception {
			// when & then
			mockMvc.perform(get("/api/{uuid}/letters/{day}", user.getShareUuid(), 0)
					.cookie(new Cookie("accessToken", accessToken)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.message").value("날짜는 1일부터 25일까지입니다"));
		}

		@Test
		@DisplayName("잘못된 날짜 범위 - 26일")
		void getLettersByDay_InvalidDay_Over25_Returns400() throws Exception {
			// when & then
			mockMvc.perform(get("/api/{uuid}/letters/{day}", user.getShareUuid(), 26)
					.cookie(new Cookie("accessToken", accessToken)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.message").value("날짜는 1일부터 25일까지입니다"));
		}
	}

	@Nested
	@DisplayName("GET /api/{uuid}/letters/count - 날짜별 편지 개수 조회")
	class GetLetterCounts {

		@BeforeEach
		void setUpLetters() {
			for (int i = 1; i <= 3; i++) {
				Letter letter = Letter.builder()
					.user(user)
					.day(1)
					.content("1일 편지 " + i)
					.fromName("친구" + i)
					.build();
				letterRepository.save(letter);
			}

			for (int i = 1; i <= 2; i++) {
				Letter letter = Letter.builder()
					.user(user)
					.day(10)
					.content("10일 편지 " + i)
					.fromName("친구" + i)
					.build();
				letterRepository.save(letter);
			}

			Letter letter25 = Letter.builder()
				.user(user)
				.day(25)
				.content("크리스마스!")
				.fromName("산타")
				.build();
			letterRepository.save(letter25);
		}

		@Test
		@DisplayName("날짜별 편지 개수 조회 성공 (인증 불필요)")
		void getLetterCounts_Success() throws Exception {
			// when & then
			mockMvc.perform(get("/api/{uuid}/letters/count", user.getShareUuid()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.data.counts").isMap())
				.andExpect(jsonPath("$.data.counts['1']").value(3))
				.andExpect(jsonPath("$.data.counts['10']").value(2))
				.andExpect(jsonPath("$.data.counts['25']").value(1))
				.andExpect(jsonPath("$.data.counts['2']").value(0))
				.andExpect(jsonPath("$.data.counts['5']").value(0));
		}

		@Test
		@DisplayName("편지가 없는 경우 모든 날짜 0으로 반환")
		void getLetterCounts_NoLetters_ReturnsAllZeros() throws Exception {
			// given
			letterRepository.deleteAll();

			// when & then
			mockMvc.perform(get("/api/{uuid}/letters/count", user.getShareUuid()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.data.counts").isMap())
				.andExpect(jsonPath("$.data.counts['1']").value(0))
				.andExpect(jsonPath("$.data.counts['10']").value(0))
				.andExpect(jsonPath("$.data.counts['25']").value(0));
		}


		@Test
		@DisplayName("존재하지 않는 UUID는 404 에러")
		void getLetterCounts_InvalidUuid_Returns404() throws Exception {
			// when & then
			mockMvc.perform(get("/api/{uuid}/letters/count", "invalid-uuid"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.message").value("존재하지 않는 사용자입니다"));
		}

		@Test
		@DisplayName("다른 사용자의 편지 개수도 조회 가능 (공개 API)")
		void getLetterCounts_OtherUser_Success() throws Exception {
			// given - anotherUser에게도 편지 작성
			Letter anotherLetter = Letter.builder()
				.user(anotherUser)
				.day(5)
				.content("다른 사용자 편지")
				.fromName("친구")
				.build();
			letterRepository.save(anotherLetter);

			// when & then
			mockMvc.perform(get("/api/{uuid}/letters/count", anotherUser.getShareUuid()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.data.counts['5']").value(1))
				.andExpect(jsonPath("$.data.counts['1']").value(0));
		}
	}

	@Nested
	@DisplayName("편지 기능 통합 플로우")
	class LetterFlow {

		@Test
		@DisplayName("전체 플로우 - 편지 작성 → 개수 확인 → 편지 조회")
		void letterFlow_Complete() throws Exception {
			// 1. 비회원이 편지 작성
			LetterCreateRequest request1 = new LetterCreateRequest(10, "첫 번째", "친구1");
			LetterCreateRequest request2 = new LetterCreateRequest(10, "두 번째", "친구2");
			LetterCreateRequest request3 = new LetterCreateRequest(15, "세 번째", "친구3");

			mockMvc.perform(post("/api/{uuid}/letters", user.getShareUuid())
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request1)))
				.andExpect(status().isOk());

			mockMvc.perform(post("/api/{uuid}/letters", user.getShareUuid())
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request2)))
				.andExpect(status().isOk());

			mockMvc.perform(post("/api/{uuid}/letters", user.getShareUuid())
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request3)))
				.andExpect(status().isOk());

			// 2. 편지 개수 확인 (비회원도 가능)
			mockMvc.perform(get("/api/{uuid}/letters/count", user.getShareUuid()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.counts['10']").value(2))
				.andExpect(jsonPath("$.data.counts['15']").value(1));

			// 3. 본인이 편지 조회
			mockMvc.perform(get("/api/{uuid}/letters", user.getShareUuid())
					.cookie(new Cookie("accessToken", accessToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(3));

			// 4. 특정 날짜 편지 조회
			mockMvc.perform(get("/api/{uuid}/letters/{day}", user.getShareUuid(), 10)
					.cookie(new Cookie("accessToken", accessToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(2));
		}
	}
}
