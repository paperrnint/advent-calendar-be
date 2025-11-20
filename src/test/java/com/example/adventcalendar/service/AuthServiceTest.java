package com.example.adventcalendar.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.adventcalendar.config.JwtTokenProvider;
import com.example.adventcalendar.constant.UserStatus;
import com.example.adventcalendar.dto.request.UserCreateRequest;
import com.example.adventcalendar.dto.response.LoginResponse;
import com.example.adventcalendar.dto.response.UserRegistrationResult;
import com.example.adventcalendar.entity.RefreshToken;
import com.example.adventcalendar.entity.User;
import com.example.adventcalendar.exception.ConflictException;
import com.example.adventcalendar.exception.ResourceNotFoundException;
import com.example.adventcalendar.exception.UnauthorizedException;
import com.example.adventcalendar.repository.RefreshTokenRepository;
import com.example.adventcalendar.repository.UserRepository;
import com.example.adventcalendar.service.OAuth2Service.OAuthUserInfo;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 단위 테스트")
class AuthServiceTest {

	@Mock
	private OAuth2Service oAuth2Service;

	@Mock
	private JwtTokenProvider jwtTokenProvider;

	@Mock
	private UserRepository userRepository;

	@Mock
	private RefreshTokenRepository refreshTokenRepository;

	@InjectMocks
	private AuthService authService;

	private User activeUser;
	private User pendingUser;
	private OAuthUserInfo oAuthUserInfo;

	@BeforeEach
	void setUp() {
		activeUser = User.builder()
			.id(1L)
			.email("test@example.com")
			.name("테스트")
			.oauthProvider("NAVER")
			.oauthId("naver123")
			.selectedColor("green")
			.shareUuid("test-uuid-123")
			.status(UserStatus.ACTIVE)
			.build();

		pendingUser = User.builder()
			.id(2L)
			.email("pending@example.com")
			.name("대기중")
			.oauthProvider("KAKAO")
			.oauthId("kakao456")
			.status(UserStatus.PENDING)
			.build();

		oAuthUserInfo = OAuthUserInfo.builder()
			.oauthProvider("NAVER")
			.oauthId("naver123")
			.email("test@example.com")
			.name("테스트")
			.profileImageUrl("http://example.com/image.jpg")
			.build();
	}

	@Nested
	@DisplayName("네이버 OAuth 콜백 처리")
	class HandleNaverCallback {

		@Test
		@DisplayName("기존 ACTIVE 사용자 로그인 성공")
		void handleNaverCallback_ExistingActiveUser_Success() {
			// given
			String code = "auth-code";
			String state = "state-value";
			String accessToken = "access-token";
			String refreshToken = "refresh-token";

			given(oAuth2Service.authenticateNaver(code, state)).willReturn(oAuthUserInfo);
			given(userRepository.findByOauthProviderAndOauthId("NAVER", "naver123"))
				.willReturn(Optional.of(activeUser));
			given(jwtTokenProvider.createAccessToken(1L, "test@example.com", "NAVER"))
				.willReturn(accessToken);
			given(jwtTokenProvider.createRefreshToken(1L)).willReturn(refreshToken);
			given(jwtTokenProvider.getAccessTokenValidityInSeconds()).willReturn(3600L);

			// when
			LoginResponse response = authService.handleNaverCallback(code, state);

			// then
			assertThat(response).isNotNull();
			assertThat(response.isExistingUser()).isTrue();
			assertThat(response.accessToken()).isEqualTo(accessToken);
			assertThat(response.refreshToken()).isEqualTo(refreshToken);
			assertThat(response.userUuid()).isEqualTo("test-uuid-123");
			assertThat(response.expiresIn()).isEqualTo(3600L);

			verify(refreshTokenRepository).save(any(RefreshToken.class));
		}

		@Test
		@DisplayName("기존 PENDING 사용자는 임시 토큰 발급")
		void handleNaverCallback_ExistingPendingUser_ReturnsTempToken() {
			// given
			String code = "auth-code";
			String state = "state-value";
			String tempToken = "temp-token";

			given(oAuth2Service.authenticateNaver(code, state)).willReturn(oAuthUserInfo);
			given(userRepository.findByOauthProviderAndOauthId("NAVER", "naver123"))
				.willReturn(Optional.of(pendingUser));
			given(jwtTokenProvider.createTempToken(2L, "pending@example.com", "KAKAO"))
				.willReturn(tempToken);

			// when
			LoginResponse response = authService.handleNaverCallback(code, state);

			// then
			assertThat(response).isNotNull();
			assertThat(response.isExistingUser()).isFalse();
			assertThat(response.accessToken()).isEqualTo(tempToken);
			assertThat(response.refreshToken()).isNull();
			assertThat(response.userUuid()).isNull();
			assertThat(response.expiresIn()).isEqualTo(300L);

			verify(refreshTokenRepository, never()).save(any());
		}

		@Test
		@DisplayName("신규 사용자는 PENDING 상태로 생성 후 임시 토큰 발급")
		void handleNaverCallback_NewUser_CreatesPendingUserAndReturnsTempToken() {
			// given
			String code = "auth-code";
			String state = "state-value";
			String tempToken = "temp-token";

			User newPendingUser = User.builder()
				.id(3L)
				.email("test@example.com")
				.name("테스트")
				.oauthProvider("NAVER")
				.oauthId("naver123")
				.status(UserStatus.PENDING)
				.build();

			given(oAuth2Service.authenticateNaver(code, state)).willReturn(oAuthUserInfo);
			given(userRepository.findByOauthProviderAndOauthId("NAVER", "naver123"))
				.willReturn(Optional.empty());
			given(userRepository.save(any(User.class))).willReturn(newPendingUser);
			given(jwtTokenProvider.createTempToken(3L, "test@example.com", "NAVER"))
				.willReturn(tempToken);

			// when
			LoginResponse response = authService.handleNaverCallback(code, state);

			// then
			assertThat(response).isNotNull();
			assertThat(response.isExistingUser()).isFalse();
			assertThat(response.accessToken()).isEqualTo(tempToken);
			assertThat(response.refreshToken()).isNull();
			assertThat(response.userUuid()).isNull();

			verify(userRepository).save(argThat(user ->
				user.getStatus() == UserStatus.PENDING &&
					user.getEmail().equals("test@example.com") &&
					user.getOauthProvider().equals("NAVER")
			));
		}
	}

	@Nested
	@DisplayName("카카오 OAuth 콜백 처리")
	class HandleKakaoCallback {

		@Test
		@DisplayName("기존 ACTIVE 사용자 로그인 성공")
		void handleKakaoCallback_ExistingActiveUser_Success() {
			// given
			String code = "auth-code";
			String accessToken = "access-token";
			String refreshToken = "refresh-token";

			OAuthUserInfo kakaoUserInfo = OAuthUserInfo.builder()
				.oauthProvider("KAKAO")
				.oauthId("kakao123")
				.email("kakao@example.com")
				.name("카카오")
				.build();

			User kakaoUser = User.builder()
				.id(1L)
				.email("kakao@example.com")
				.name("카카오")
				.oauthProvider("KAKAO")
				.oauthId("kakao123")
				.shareUuid("kakao-uuid")
				.status(UserStatus.ACTIVE)
				.build();

			given(oAuth2Service.authenticateKakao(code)).willReturn(kakaoUserInfo);
			given(userRepository.findByOauthProviderAndOauthId("KAKAO", "kakao123"))
				.willReturn(Optional.of(kakaoUser));
			given(jwtTokenProvider.createAccessToken(1L, "kakao@example.com", "KAKAO"))
				.willReturn(accessToken);
			given(jwtTokenProvider.createRefreshToken(1L)).willReturn(refreshToken);
			given(jwtTokenProvider.getAccessTokenValidityInSeconds()).willReturn(3600L);

			// when
			LoginResponse response = authService.handleKakaoCallback(code);

			// then
			assertThat(response).isNotNull();
			assertThat(response.isExistingUser()).isTrue();
			assertThat(response.accessToken()).isEqualTo(accessToken);
			assertThat(response.refreshToken()).isEqualTo(refreshToken);
			assertThat(response.userUuid()).isEqualTo("kakao-uuid");
		}

		@Test
		@DisplayName("신규 사용자는 PENDING 상태로 생성")
		void handleKakaoCallback_NewUser_CreatesPendingUser() {
			// given
			String code = "auth-code";
			String tempToken = "temp-token";

			OAuthUserInfo kakaoUserInfo = OAuthUserInfo.builder()
				.oauthProvider("KAKAO")
				.oauthId("kakao999")
				.email("new@example.com")
				.name("신규")
				.build();

			User newUser = User.builder()
				.id(99L)
				.email("new@example.com")
				.name("신규")
				.oauthProvider("KAKAO")
				.oauthId("kakao999")
				.status(UserStatus.PENDING)
				.build();

			given(oAuth2Service.authenticateKakao(code)).willReturn(kakaoUserInfo);
			given(userRepository.findByOauthProviderAndOauthId("KAKAO", "kakao999"))
				.willReturn(Optional.empty());
			given(userRepository.save(any(User.class))).willReturn(newUser);
			given(jwtTokenProvider.createTempToken(99L, "new@example.com", "KAKAO"))
				.willReturn(tempToken);

			// when
			LoginResponse response = authService.handleKakaoCallback(code);

			// then
			assertThat(response).isNotNull();
			assertThat(response.isExistingUser()).isFalse();
			assertThat(response.accessToken()).isEqualTo(tempToken);

			verify(userRepository).save(any(User.class));
		}
	}

	@Nested
	@DisplayName("사용자 등록 완료")
	class CompleteUserRegistration {

		@Test
		@DisplayName("PENDING 사용자 정상 등록")
		void completeUserRegistration_PendingUser_Success() {
			// given
			Long userId = 2L;
			UserCreateRequest request = new UserCreateRequest("민완", "green");
			String accessToken = "access-token";
			String refreshToken = "refresh-token";

			given(userRepository.findById(userId)).willReturn(Optional.of(pendingUser));
			given(userRepository.save(any(User.class))).willAnswer(invocation -> {
				User user = invocation.getArgument(0);
				user.setShareUuid("generated-uuid");
				return user;
			});
			given(jwtTokenProvider.createAccessToken(userId, "pending@example.com", "KAKAO"))
				.willReturn(accessToken);
			given(jwtTokenProvider.createRefreshToken(userId)).willReturn(refreshToken);

			// when
			UserRegistrationResult result = authService.completeUserRegistration(userId, request);

			// then
			assertThat(result).isNotNull();
			assertThat(result.uuid()).isNotNull();
			assertThat(result.accessToken()).isEqualTo(accessToken);
			assertThat(result.refreshToken()).isEqualTo(refreshToken);

			verify(userRepository).save(argThat(user ->
				user.getStatus() == UserStatus.ACTIVE &&
					user.getName().equals("민완") &&
					user.getSelectedColor().equals("green") &&
					user.getShareUuid() != null
			));
			verify(refreshTokenRepository).deleteByUserId(userId);
			verify(refreshTokenRepository).save(any(RefreshToken.class));
		}

		@Test
		@DisplayName("존재하지 않는 사용자 예외 발생")
		void completeUserRegistration_UserNotFound_ThrowsException() {
			// given
			Long userId = 999L;
			UserCreateRequest request = new UserCreateRequest("테스트", "blue");

			given(userRepository.findById(userId)).willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> authService.completeUserRegistration(userId, request))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("사용자를 찾을 수 없습니다");
		}

		@Test
		@DisplayName("이미 ACTIVE 상태인 사용자 예외 발생")
		void completeUserRegistration_AlreadyActive_ThrowsException() {
			// given
			Long userId = 1L;
			UserCreateRequest request = new UserCreateRequest("테스트", "blue");

			given(userRepository.findById(userId)).willReturn(Optional.of(activeUser));

			// when & then
			assertThatThrownBy(() -> authService.completeUserRegistration(userId, request))
				.isInstanceOf(ConflictException.class)
				.hasMessage("이미 등록이 완료된 사용자입니다");
		}

		@Test
		@DisplayName("XSS 방어 - HTML 이스케이프 적용")
		void completeUserRegistration_XssDefense_EscapesHtml() {
			// given
			Long userId = 2L;
			UserCreateRequest request = new UserCreateRequest("<script>alert('xss')</script>", "green");

			given(userRepository.findById(userId)).willReturn(Optional.of(pendingUser));
			given(userRepository.save(any(User.class))).willAnswer(invocation -> {
				User user = invocation.getArgument(0);
				user.setShareUuid("test-uuid");
				return user;
			});
			given(jwtTokenProvider.createAccessToken(anyLong(), anyString(), anyString()))
				.willReturn("token");
			given(jwtTokenProvider.createRefreshToken(anyLong())).willReturn("refresh");

			// when
			authService.completeUserRegistration(userId, request);

			// then
			verify(userRepository).save(argThat(user ->
				!user.getName().contains("<script>") &&
					user.getName().contains("&lt;") // HTML escaped
			));
		}
	}

	@Nested
	@DisplayName("토큰 갱신")
	class RefreshAccessToken {

		@Test
		@DisplayName("유효한 RefreshToken으로 AccessToken 갱신 성공")
		void refreshAccessToken_ValidToken_Success() {
			// given
			String refreshToken = "valid-refresh-token";
			String newAccessToken = "new-access-token";

			RefreshToken storedToken = RefreshToken.builder()
				.id(1L)
				.userId(1L)
				.token(refreshToken)
				.expiresAt(LocalDateTime.now().plusDays(7))
				.build();

			given(jwtTokenProvider.validateToken(refreshToken)).willReturn(true);
			given(refreshTokenRepository.findByToken(refreshToken))
				.willReturn(Optional.of(storedToken));
			given(userRepository.findById(1L)).willReturn(Optional.of(activeUser));
			given(jwtTokenProvider.createAccessToken(1L, "test@example.com", "NAVER"))
				.willReturn(newAccessToken);

			// when
			String result = authService.refreshAccessToken(refreshToken);

			// then
			assertThat(result).isEqualTo(newAccessToken);
		}

		@Test
		@DisplayName("유효하지 않은 토큰 예외 발생")
		void refreshAccessToken_InvalidToken_ThrowsException() {
			// given
			String refreshToken = "invalid-token";

			given(jwtTokenProvider.validateToken(refreshToken)).willReturn(false);

			// when & then
			assertThatThrownBy(() -> authService.refreshAccessToken(refreshToken))
				.isInstanceOf(UnauthorizedException.class)
				.hasMessage("유효하지 않은 RefreshToken입니다");
		}

		@Test
		@DisplayName("존재하지 않는 RefreshToken 예외 발생")
		void refreshAccessToken_TokenNotFound_ThrowsException() {
			// given
			String refreshToken = "unknown-token";

			given(jwtTokenProvider.validateToken(refreshToken)).willReturn(true);
			given(refreshTokenRepository.findByToken(refreshToken))
				.willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> authService.refreshAccessToken(refreshToken))
				.isInstanceOf(UnauthorizedException.class)
				.hasMessage("RefreshToken을 찾을 수 없습니다");
		}

		@Test
		@DisplayName("만료된 RefreshToken 예외 발생 및 삭제")
		void refreshAccessToken_ExpiredToken_ThrowsExceptionAndDeletes() {
			// given
			String refreshToken = "expired-token";

			RefreshToken storedToken = RefreshToken.builder()
				.id(1L)
				.userId(1L)
				.token(refreshToken)
				.expiresAt(LocalDateTime.now().minusDays(1)) // 만료됨
				.build();

			given(jwtTokenProvider.validateToken(refreshToken)).willReturn(true);
			given(refreshTokenRepository.findByToken(refreshToken))
				.willReturn(Optional.of(storedToken));

			// when & then
			assertThatThrownBy(() -> authService.refreshAccessToken(refreshToken))
				.isInstanceOf(UnauthorizedException.class)
				.hasMessage("RefreshToken이 만료되었습니다");

			verify(refreshTokenRepository).delete(storedToken);
		}

		@Test
		@DisplayName("사용자를 찾을 수 없는 경우 예외 발생")
		void refreshAccessToken_UserNotFound_ThrowsException() {
			// given
			String refreshToken = "valid-token";

			RefreshToken storedToken = RefreshToken.builder()
				.id(1L)
				.userId(999L) // 존재하지 않는 사용자
				.token(refreshToken)
				.expiresAt(LocalDateTime.now().plusDays(7))
				.build();

			given(jwtTokenProvider.validateToken(refreshToken)).willReturn(true);
			given(refreshTokenRepository.findByToken(refreshToken))
				.willReturn(Optional.of(storedToken));
			given(userRepository.findById(999L)).willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> authService.refreshAccessToken(refreshToken))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("사용자를 찾을 수 없습니다");
		}
	}

	@Nested
	@DisplayName("로그아웃")
	class Logout {

		@Test
		@DisplayName("RefreshToken 삭제 성공")
		void logout_ValidToken_DeletesToken() {
			// given
			String refreshToken = "refresh-token";

			RefreshToken storedToken = RefreshToken.builder()
				.id(1L)
				.userId(1L)
				.token(refreshToken)
				.expiresAt(LocalDateTime.now().plusDays(7))
				.build();

			given(refreshTokenRepository.findByToken(refreshToken))
				.willReturn(Optional.of(storedToken));

			// when
			authService.logout(refreshToken);

			// then
			verify(refreshTokenRepository).delete(storedToken);
			verify(refreshTokenRepository).flush();
		}

		@Test
		@DisplayName("존재하지 않는 토큰도 예외 없이 처리")
		void logout_TokenNotFound_NoException() {
			// given
			String refreshToken = "unknown-token";

			given(refreshTokenRepository.findByToken(refreshToken))
				.willReturn(Optional.empty());

			// when & then
			assertThatCode(() -> authService.logout(refreshToken))
				.doesNotThrowAnyException();

			verify(refreshTokenRepository, never()).delete(any());
		}
	}

	@Nested
	@DisplayName("사용자 활성화 상태 검증")
	class ValidateUserActive {

		@Test
		@DisplayName("ACTIVE 상태 사용자 검증 통과")
		void validateUserActive_ActiveUser_Success() {
			// given
			Long userId = 1L;

			given(userRepository.findById(userId)).willReturn(Optional.of(activeUser));

			// when & then
			assertThatCode(() -> authService.validateUserActive(userId))
				.doesNotThrowAnyException();
		}

		@Test
		@DisplayName("PENDING 상태 사용자 예외 발생")
		void validateUserActive_PendingUser_ThrowsException() {
			// given
			Long userId = 2L;

			given(userRepository.findById(userId)).willReturn(Optional.of(pendingUser));

			// when & then
			assertThatThrownBy(() -> authService.validateUserActive(userId))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("회원가입을 완료해주세요");
		}

		@Test
		@DisplayName("존재하지 않는 사용자 예외 발생")
		void validateUserActive_UserNotFound_ThrowsException() {
			// given
			Long userId = 999L;

			given(userRepository.findById(userId)).willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> authService.validateUserActive(userId))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("사용자를 찾을 수 없습니다");
		}
	}
}
