package com.example.adventcalendar.controller;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.example.adventcalendar.config.JwtTokenProvider;
import com.example.adventcalendar.constant.UserStatus;
import com.example.adventcalendar.dto.request.UserCreateRequest;
import com.example.adventcalendar.entity.Letter;
import com.example.adventcalendar.entity.RefreshToken;
import com.example.adventcalendar.entity.User;
import com.example.adventcalendar.repository.LetterRepository;
import com.example.adventcalendar.repository.RefreshTokenRepository;
import com.example.adventcalendar.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.Cookie;

import java.time.LocalDateTime;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("AuthController 통합 테스트")
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private LetterRepository letterRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	private User activeUser;
	private User pendingUser;

	@BeforeEach
	void setUp() {
		letterRepository.deleteAll();
		refreshTokenRepository.deleteAll();
		userRepository.deleteAll();

		activeUser = User.builder()
			.email("active@example.com")
			.name("활성사용자")
			.oauthProvider("NAVER")
			.oauthId("naver123")
			.selectedColor("green")
			.shareUuid("active-uuid-123")
			.status(UserStatus.ACTIVE)
			.build();
		activeUser = userRepository.save(activeUser);

		pendingUser = User.builder()
			.email("pending@example.com")
			.name("대기사용자")
			.oauthProvider("KAKAO")
			.oauthId("kakao456")
			.status(UserStatus.PENDING)
			.build();
		pendingUser = userRepository.save(pendingUser);
	}

	@Nested
	@DisplayName("POST /api/auth/users - 신규 사용자 등록")
	class CreateUser {

		@Test
		@DisplayName("PENDING 사용자 등록 성공")
		void createUser_PendingUser_Success() throws Exception {
			// given
			String tempToken = jwtTokenProvider.createTempToken(
				pendingUser.getId(),
				pendingUser.getEmail(),
				pendingUser.getOauthProvider()
			);

			UserCreateRequest request = new UserCreateRequest("민완", "green");

			// when & then
			MvcResult result = mockMvc.perform(post("/api/auth/users")
					.cookie(new Cookie("tempToken", tempToken))
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.message").value("회원가입이 완료되었습니다"))
				.andExpect(jsonPath("$.data.uuid").exists())
				.andExpect(cookie().exists("accessToken"))
				.andExpect(cookie().exists("refreshToken"))
				.andReturn();

			// DB 검증
			User updatedUser = userRepository.findById(pendingUser.getId()).orElseThrow();
			assertThat(updatedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
			assertThat(updatedUser.getName()).isEqualTo("민완");
			assertThat(updatedUser.getSelectedColor()).isEqualTo("green");
			assertThat(updatedUser.getShareUuid()).isNotNull();

			// RefreshToken 저장 확인
			assertThat(refreshTokenRepository.findByUserId(pendingUser.getId())).isPresent();
		}

		@Test
		@DisplayName("이미 ACTIVE 상태인 사용자는 예외 발생")
		void createUser_AlreadyActive_ThrowsException() throws Exception {
			// given
			String tempToken = jwtTokenProvider.createTempToken(
				activeUser.getId(),
				activeUser.getEmail(),
				activeUser.getOauthProvider()
			);

			UserCreateRequest request = new UserCreateRequest("테스트", "blue");

			// when & then
			mockMvc.perform(post("/api/auth/users")
					.cookie(new Cookie("tempToken", tempToken))
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.message").value("이미 등록이 완료된 사용자입니다"))
				.andExpect(jsonPath("$.errorCode").value("CONFLICT"));
		}

		@Test
		@DisplayName("인증되지 않은 요청은 401 에러")
		void createUser_Unauthorized_Returns401() throws Exception {
			// given
			UserCreateRequest request = new UserCreateRequest("테스트", "blue");

			// when & then
			mockMvc.perform(post("/api/auth/users")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.message").value("인증이 필요합니다"));
		}

		@Test
		@DisplayName("이름 validation 실패 - 빈 값")
		void createUser_EmptyName_ValidationFailed() throws Exception {
			// given
			String tempToken = jwtTokenProvider.createTempToken(
				pendingUser.getId(),
				pendingUser.getEmail(),
				pendingUser.getOauthProvider()
			);

			UserCreateRequest request = new UserCreateRequest("", "green");

			// when & then
			mockMvc.perform(post("/api/auth/users")
					.cookie(new Cookie("tempToken", tempToken))
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
		}

		@Test
		@DisplayName("색상 validation 실패 - 잘못된 색상")
		void createUser_InvalidColor_ValidationFailed() throws Exception {
			// given
			String tempToken = jwtTokenProvider.createTempToken(
				pendingUser.getId(),
				pendingUser.getEmail(),
				pendingUser.getOauthProvider()
			);

			UserCreateRequest request = new UserCreateRequest("민완", "invalid-color");

			// when & then
			mockMvc.perform(post("/api/auth/users")
					.cookie(new Cookie("tempToken", tempToken))
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
		}

		@Test
		@DisplayName("XSS 방어 - HTML 태그 이스케이프")
		void createUser_XssDefense_EscapesHtml() throws Exception {
			// given
			String tempToken = jwtTokenProvider.createTempToken(
				pendingUser.getId(),
				pendingUser.getEmail(),
				pendingUser.getOauthProvider()
			);

			// 이름 길이 제한(10자) 내에서 XSS 테스트
			UserCreateRequest request = new UserCreateRequest("<b>민완</b>", "green");

			// when
			mockMvc.perform(post("/api/auth/users")
					.cookie(new Cookie("tempToken", tempToken))
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk());

			// then
			User updatedUser = userRepository.findById(pendingUser.getId()).orElseThrow();
			assertThat(updatedUser.getName()).doesNotContain("<b>");
			assertThat(updatedUser.getName()).contains("&lt;b&gt;");
			assertThat(updatedUser.getName()).contains("민완");
			assertThat(updatedUser.getName()).contains("&lt;/b&gt;");
		}
	}

	@Nested
	@DisplayName("GET /api/auth/me - 현재 사용자 정보 조회")
	class GetCurrentUser {

		@Test
		@DisplayName("ACTIVE 사용자 정보 조회 성공")
		void getCurrentUser_ActiveUser_Success() throws Exception {
			// given
			String accessToken = jwtTokenProvider.createAccessToken(
				activeUser.getId(),
				activeUser.getEmail(),
				activeUser.getOauthProvider()
			);

			// when & then
			mockMvc.perform(get("/api/auth/me")
					.cookie(new Cookie("accessToken", accessToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.data.name").value("활성사용자"))
				.andExpect(jsonPath("$.data.color").value("green"))
				.andExpect(jsonPath("$.data.uuid").value("active-uuid-123"));
		}

		@Test
		@DisplayName("PENDING 사용자는 예외 발생")
		void getCurrentUser_PendingUser_ThrowsException() throws Exception {
			// given
			String accessToken = jwtTokenProvider.createAccessToken(
				pendingUser.getId(),
				pendingUser.getEmail(),
				pendingUser.getOauthProvider()
			);

			// when & then
			mockMvc.perform(get("/api/auth/me")
					.cookie(new Cookie("accessToken", accessToken)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.message").value("회원가입을 완료해주세요"));
		}

		@Test
		@DisplayName("인증되지 않은 요청은 401 에러")
		void getCurrentUser_Unauthorized_Returns401() throws Exception {
			// when & then
			mockMvc.perform(get("/api/auth/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.message").value("인증이 필요합니다"));
		}

		@Test
		@DisplayName("존재하지 않는 사용자는 404 에러")
		void getCurrentUser_UserNotFound_Returns404() throws Exception {
			// given
			String accessToken = jwtTokenProvider.createAccessToken(
				999L,
				"nonexistent@example.com",
				"NAVER"
			);

			// when & then
			mockMvc.perform(get("/api/auth/me")
					.cookie(new Cookie("accessToken", accessToken)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.message").value("존재하지 않는 사용자입니다"));
		}
	}

	@Nested
	@DisplayName("POST /api/auth/refresh - 토큰 갱신")
	class RefreshTokenTest {

		@Test
		@DisplayName("유효한 RefreshToken으로 AccessToken 갱신 성공")
		void refreshToken_ValidToken_Success() throws Exception {
			// given
			String refreshToken = jwtTokenProvider.createRefreshToken(activeUser.getId());

			RefreshToken storedToken = new RefreshToken();
			storedToken.setUserId(activeUser.getId());
			storedToken.setToken(refreshToken);
			storedToken.setExpiresAt(LocalDateTime.now().plusDays(7));
			refreshTokenRepository.save(storedToken);

			// when & then
			mockMvc.perform(post("/api/auth/refresh")
					.cookie(new Cookie("refreshToken", refreshToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(cookie().exists("accessToken"));
		}

		@Test
		@DisplayName("유효하지 않은 RefreshToken은 401 에러")
		void refreshToken_InvalidToken_Returns401() throws Exception {
			// given
			String invalidToken = "invalid-refresh-token";

			// when & then
			mockMvc.perform(post("/api/auth/refresh")
					.cookie(new Cookie("refreshToken", invalidToken)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.message").value("유효하지 않은 RefreshToken입니다"));
		}

		@Test
		@DisplayName("만료된 RefreshToken은 401 에러")
		void refreshToken_ExpiredToken_Returns401() throws Exception {
			// given
			String refreshToken = jwtTokenProvider.createRefreshToken(activeUser.getId());

			RefreshToken storedToken = new RefreshToken();
			storedToken.setUserId(activeUser.getId());
			storedToken.setToken(refreshToken);
			storedToken.setExpiresAt(LocalDateTime.now().minusDays(1)); // 만료됨
			refreshTokenRepository.save(storedToken);

			// when & then
			mockMvc.perform(post("/api/auth/refresh")
					.cookie(new Cookie("refreshToken", refreshToken)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.message").value("RefreshToken이 만료되었습니다"));

			// 만료된 토큰은 삭제되어야 함
			assertThat(refreshTokenRepository.findByToken(refreshToken)).isEmpty();
		}

		@Test
		@DisplayName("존재하지 않는 RefreshToken은 401 에러")
		void refreshToken_TokenNotFound_Returns401() throws Exception {
			// given
			String refreshToken = jwtTokenProvider.createRefreshToken(activeUser.getId());

			// when & then
			mockMvc.perform(post("/api/auth/refresh")
					.cookie(new Cookie("refreshToken", refreshToken)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.message").value("RefreshToken을 찾을 수 없습니다"));
		}

		@Test
		@DisplayName("RefreshToken 쿠키가 없으면 401 에러")
		void refreshToken_NoCookie_Returns401() throws Exception {
			// when & then
			mockMvc.perform(post("/api/auth/refresh"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.message").value("RefreshToken이 필요합니다"));
		}
	}

	@Nested
	@DisplayName("POST /api/auth/logout - 로그아웃")
	class Logout {

		@Test
		@DisplayName("로그아웃 성공 - RefreshToken 삭제 및 쿠키 제거")
		void logout_Success() throws Exception {
			// given
			String refreshToken = jwtTokenProvider.createRefreshToken(activeUser.getId());

			RefreshToken storedToken = new RefreshToken();
			storedToken.setUserId(activeUser.getId());
			storedToken.setToken(refreshToken);
			storedToken.setExpiresAt(LocalDateTime.now().plusDays(7));
			refreshTokenRepository.save(storedToken);

			// when & then
			mockMvc.perform(post("/api/auth/logout")
					.cookie(new Cookie("refreshToken", refreshToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(cookie().maxAge("refreshToken", 0))
				.andExpect(cookie().maxAge("accessToken", 0));

			// RefreshToken이 삭제되었는지 확인
			assertThat(refreshTokenRepository.findByToken(refreshToken)).isEmpty();
		}

		@Test
		@DisplayName("RefreshToken 없이 로그아웃 - 정상 처리")
		void logout_NoRefreshToken_Success() throws Exception {
			// when & then
			mockMvc.perform(post("/api/auth/logout"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(cookie().maxAge("refreshToken", 0))
				.andExpect(cookie().maxAge("accessToken", 0));
		}

		@Test
		@DisplayName("존재하지 않는 RefreshToken으로 로그아웃 - 정상 처리")
		void logout_NonexistentToken_Success() throws Exception {
			// given
			String nonexistentToken = "nonexistent-refresh-token";

			// when & then
			mockMvc.perform(post("/api/auth/logout")
					.cookie(new Cookie("refreshToken", nonexistentToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200));
		}
	}

	@Nested
	@DisplayName("DELETE /api/auth/users - 회원 탈퇴")
	class DeleteUser {

		@Test
		@DisplayName("회원 탈퇴 성공")
		void deleteUser_Success() throws Exception {
			// given
			String accessToken = jwtTokenProvider.createAccessToken(
				activeUser.getId(),
				activeUser.getEmail(),
				activeUser.getOauthProvider()
			);

			// 편지 추가
			Letter letter = Letter.builder()
				.user(activeUser)
				.day(10)
				.content("테스트 편지")
				.fromName("친구")
				.build();
			letterRepository.save(letter);

			// RefreshToken 추가
			RefreshToken refreshToken = RefreshToken.builder()
				.userId(activeUser.getId())
				.token("test-refresh-token")
				.expiresAt(LocalDateTime.now().plusDays(7))
				.build();
			refreshTokenRepository.save(refreshToken);

			// when & then
			mockMvc.perform(delete("/api/auth/users")
					.cookie(new Cookie("accessToken", accessToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(cookie().maxAge("refreshToken", 0))
				.andExpect(cookie().maxAge("accessToken", 0));

			// DB 검증 - 모든 데이터 삭제 확인
			assertThat(userRepository.findById(activeUser.getId())).isEmpty();
			assertThat(letterRepository.findByUserId(activeUser.getId())).isEmpty();
			assertThat(refreshTokenRepository.findByUserId(activeUser.getId())).isEmpty();
		}

		@Test
		@DisplayName("인증되지 않은 요청은 401 에러")
		void deleteUser_Unauthorized_Returns401() throws Exception {
			// when & then
			mockMvc.perform(delete("/api/auth/users"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.message").value("인증이 필요합니다"));
		}

		@Test
		@DisplayName("존재하지 않는 사용자는 404 에러")
		void deleteUser_UserNotFound_Returns404() throws Exception {
			// given
			String accessToken = jwtTokenProvider.createAccessToken(
				999L,
				"nonexistent@example.com",
				"NAVER"
			);

			// when & then
			mockMvc.perform(delete("/api/auth/users")
					.cookie(new Cookie("accessToken", accessToken)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.message").value("사용자를 찾을 수 없습니다"));
		}

		@Test
		@DisplayName("PENDING 상태 사용자도 탈퇴 가능")
		void deleteUser_PendingUser_Success() throws Exception {
			// given
			String accessToken = jwtTokenProvider.createAccessToken(
				pendingUser.getId(),
				pendingUser.getEmail(),
				pendingUser.getOauthProvider()
			);

			// when & then
			mockMvc.perform(delete("/api/auth/users")
					.cookie(new Cookie("accessToken", accessToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200));

			// DB 검증
			assertThat(userRepository.findById(pendingUser.getId())).isEmpty();
		}
	}

	@Nested
	@DisplayName("인증 플로우 통합 테스트")
	class AuthenticationFlow {

		@Test
		@DisplayName("신규 사용자 전체 플로우 - tempToken → 회원가입 → accessToken")
		void newUserFlow_Complete() throws Exception {
			// 1. tempToken으로 시작
			String tempToken = jwtTokenProvider.createTempToken(
				pendingUser.getId(),
				pendingUser.getEmail(),
				pendingUser.getOauthProvider()
			);

			// 2. 회원가입
			UserCreateRequest request = new UserCreateRequest("민완", "green");

			MvcResult result = mockMvc.perform(post("/api/auth/users")
					.cookie(new Cookie("tempToken", tempToken))
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(cookie().exists("accessToken"))
				.andExpect(cookie().exists("refreshToken"))
				.andReturn();

			// 3. accessToken으로 사용자 정보 조회
			Cookie accessTokenCookie = result.getResponse().getCookie("accessToken");
			assertThat(accessTokenCookie).isNotNull();

			mockMvc.perform(get("/api/auth/me")
					.cookie(accessTokenCookie))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.name").value("민완"))
				.andExpect(jsonPath("$.data.color").value("green"));
		}

		@Test
		@DisplayName("기존 사용자 플로우 - accessToken으로 정보 조회")
		void existingUserFlow_Complete() throws Exception {
			// 1. accessToken 생성
			String accessToken = jwtTokenProvider.createAccessToken(
				activeUser.getId(),
				activeUser.getEmail(),
				activeUser.getOauthProvider()
			);

			// 2. 사용자 정보 조회
			mockMvc.perform(get("/api/auth/me")
					.cookie(new Cookie("accessToken", accessToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.name").value("활성사용자"))
				.andExpect(jsonPath("$.data.color").value("green"))
				.andExpect(jsonPath("$.data.uuid").value("active-uuid-123"));
		}
	}
}
