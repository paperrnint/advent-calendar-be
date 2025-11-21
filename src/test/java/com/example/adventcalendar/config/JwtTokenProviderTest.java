package com.example.adventcalendar.config;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;

@DisplayName("JwtTokenProvider 단위 테스트")
class JwtTokenProviderTest {

	private JwtTokenProvider jwtTokenProvider;

	private static final String SECRET = "test-secret-key-for-jwt-token-generation-must-be-long-enough-for-hs512";
	private static final long ACCESS_TOKEN_VALIDITY = 3600L; // 1시간
	private static final long REFRESH_TOKEN_VALIDITY = 604800L; // 7일

	@BeforeEach
	void setUp() {
		jwtTokenProvider = new JwtTokenProvider(
			SECRET,
			ACCESS_TOKEN_VALIDITY,
			REFRESH_TOKEN_VALIDITY
		);
	}

	@Nested
	@DisplayName("Access Token 생성")
	class CreateAccessToken {

		@Test
		@DisplayName("유효한 파라미터로 Access Token 생성 성공")
		void createAccessToken_ValidParameters_Success() {
			// given
			Long userId = 1L;
			String email = "test@example.com";
			String oauthProvider = "NAVER";

			// when
			String token = jwtTokenProvider.createAccessToken(userId, email, oauthProvider);

			// then
			assertThat(token).isNotNull();
			assertThat(token).isNotEmpty();

			Claims claims = jwtTokenProvider.parseClaims(token);
			assertThat(claims.getSubject()).isEqualTo(userId.toString());
			assertThat(claims.get("email", String.class)).isEqualTo(email);
			assertThat(claims.get("oauthProvider", String.class)).isEqualTo(oauthProvider);
			assertThat(claims.get("type", String.class)).isEqualTo("access");
		}

		@Test
		@DisplayName("Access Token에 모든 클레임 포함")
		void createAccessToken_ContainsAllClaims() {
			// given
			Long userId = 123L;
			String email = "user@test.com";
			String oauthProvider = "KAKAO";

			// when
			String token = jwtTokenProvider.createAccessToken(userId, email, oauthProvider);

			// then
			Claims claims = jwtTokenProvider.parseClaims(token);
			assertThat(claims).isNotNull();
			assertThat(claims.getSubject()).isEqualTo("123");
			assertThat(claims.get("email")).isEqualTo(email);
			assertThat(claims.get("oauthProvider")).isEqualTo(oauthProvider);
			assertThat(claims.get("type")).isEqualTo("access");
			assertThat(claims.getIssuedAt()).isNotNull();
			assertThat(claims.getExpiration()).isNotNull();
		}

		@Test
		@DisplayName("만료 시간이 올바르게 설정됨")
		void createAccessToken_ExpirationTimeSetCorrectly() {
			// given
			Long userId = 1L;
			String email = "test@example.com";
			String oauthProvider = "NAVER";

			// when
			String token = jwtTokenProvider.createAccessToken(userId, email, oauthProvider);

			// then
			Claims claims = jwtTokenProvider.parseClaims(token);
			long issuedAt = claims.getIssuedAt().getTime();
			long expiration = claims.getExpiration().getTime();
			long diff = (expiration - issuedAt) / 1000; // 초 단위

			assertThat(diff).isEqualTo(ACCESS_TOKEN_VALIDITY);
		}
	}

	@Nested
	@DisplayName("Refresh Token 생성")
	class CreateRefreshToken {

		@Test
		@DisplayName("유효한 파라미터로 Refresh Token 생성 성공")
		void createRefreshToken_ValidParameters_Success() {
			// given
			Long userId = 1L;

			// when
			String token = jwtTokenProvider.createRefreshToken(userId);

			// then
			assertThat(token).isNotNull();
			assertThat(token).isNotEmpty();

			Claims claims = jwtTokenProvider.parseClaims(token);
			assertThat(claims.getSubject()).isEqualTo(userId.toString());
			assertThat(claims.get("type", String.class)).isEqualTo("refresh");
		}

		@Test
		@DisplayName("Refresh Token에 type 클레임만 포함")
		void createRefreshToken_ContainsOnlyTypeAndSubject() {
			// given
			Long userId = 99L;

			// when
			String token = jwtTokenProvider.createRefreshToken(userId);

			// then
			Claims claims = jwtTokenProvider.parseClaims(token);
			assertThat(claims.getSubject()).isEqualTo("99");
			assertThat(claims.get("type")).isEqualTo("refresh");
			assertThat(claims.get("email")).isNull();
			assertThat(claims.get("oauthProvider")).isNull();
		}

		@Test
		@DisplayName("만료 시간이 올바르게 설정됨")
		void createRefreshToken_ExpirationTimeSetCorrectly() {
			// given
			Long userId = 1L;

			// when
			String token = jwtTokenProvider.createRefreshToken(userId);

			// then
			Claims claims = jwtTokenProvider.parseClaims(token);
			long issuedAt = claims.getIssuedAt().getTime();
			long expiration = claims.getExpiration().getTime();
			long diff = (expiration - issuedAt) / 1000;

			assertThat(diff).isEqualTo(REFRESH_TOKEN_VALIDITY);
		}
	}

	@Nested
	@DisplayName("Temp Token 생성")
	class CreateTempToken {

		@Test
		@DisplayName("유효한 파라미터로 Temp Token 생성 성공")
		void createTempToken_ValidParameters_Success() {
			// given
			Long userId = 1L;
			String email = "temp@example.com";
			String oauthProvider = "NAVER";

			// when
			String token = jwtTokenProvider.createTempToken(userId, email, oauthProvider);

			// then
			assertThat(token).isNotNull();
			assertThat(token).isNotEmpty();

			Claims claims = jwtTokenProvider.parseClaims(token);
			assertThat(claims.getSubject()).isEqualTo(userId.toString());
			assertThat(claims.get("email", String.class)).isEqualTo(email);
			assertThat(claims.get("oauthProvider", String.class)).isEqualTo(oauthProvider);
			assertThat(claims.get("type", String.class)).isEqualTo("temp");
		}

		@Test
		@DisplayName("Temp Token 만료 시간은 5분")
		void createTempToken_ExpiresInFiveMinutes() {
			// given
			Long userId = 1L;
			String email = "temp@example.com";
			String oauthProvider = "KAKAO";

			// when
			String token = jwtTokenProvider.createTempToken(userId, email, oauthProvider);

			// then
			Claims claims = jwtTokenProvider.parseClaims(token);
			long issuedAt = claims.getIssuedAt().getTime();
			long expiration = claims.getExpiration().getTime();
			long diff = (expiration - issuedAt) / 1000;

			assertThat(diff).isEqualTo(300L); // 5분 = 300초
		}
	}

	@Nested
	@DisplayName("토큰 검증")
	class ValidateToken {

		@Test
		@DisplayName("유효한 Access Token 검증 성공")
		void validateToken_ValidAccessToken_ReturnsTrue() {
			// given
			String token = jwtTokenProvider.createAccessToken(1L, "test@example.com", "NAVER");

			// when
			boolean isValid = jwtTokenProvider.validateToken(token);

			// then
			assertThat(isValid).isTrue();
		}

		@Test
		@DisplayName("유효한 Refresh Token 검증 성공")
		void validateToken_ValidRefreshToken_ReturnsTrue() {
			// given
			String token = jwtTokenProvider.createRefreshToken(1L);

			// when
			boolean isValid = jwtTokenProvider.validateToken(token);

			// then
			assertThat(isValid).isTrue();
		}

		@Test
		@DisplayName("유효한 Temp Token 검증 성공")
		void validateToken_ValidTempToken_ReturnsTrue() {
			// given
			String token = jwtTokenProvider.createTempToken(1L, "test@example.com", "KAKAO");

			// when
			boolean isValid = jwtTokenProvider.validateToken(token);

			// then
			assertThat(isValid).isTrue();
		}

		@Test
		@DisplayName("null 토큰 검증 실패")
		void validateToken_NullToken_ReturnsFalse() {
			// when
			boolean isValid = jwtTokenProvider.validateToken(null);

			// then
			assertThat(isValid).isFalse();
		}

		@Test
		@DisplayName("빈 문자열 토큰 검증 실패")
		void validateToken_EmptyToken_ReturnsFalse() {
			// when
			boolean isValid = jwtTokenProvider.validateToken("");

			// then
			assertThat(isValid).isFalse();
		}

		@Test
		@DisplayName("잘못된 형식의 토큰 검증 실패")
		void validateToken_MalformedToken_ReturnsFalse() {
			// given
			String malformedToken = "invalid.token.format";

			// when
			boolean isValid = jwtTokenProvider.validateToken(malformedToken);

			// then
			assertThat(isValid).isFalse();
		}

		@Test
		@DisplayName("잘못된 서명의 토큰 검증 실패")
		void validateToken_InvalidSignature_ReturnsFalse() {
			// given
			JwtTokenProvider anotherProvider = new JwtTokenProvider(
				"different-secret-key-for-jwt-token-generation-must-be-long-enough",
				3600L,
				604800L
			);
			String token = anotherProvider.createAccessToken(1L, "test@example.com", "NAVER");

			// when
			boolean isValid = jwtTokenProvider.validateToken(token);

			// then
			assertThat(isValid).isFalse();
		}

		@Test
		@DisplayName("만료된 토큰 검증 실패")
		void validateToken_ExpiredToken_ReturnsFalse() throws InterruptedException {
			// given
			JwtTokenProvider shortLivedProvider = new JwtTokenProvider(
				SECRET,
				1L, // 1초
				604800L
			);
			String token = shortLivedProvider.createAccessToken(1L, "test@example.com", "NAVER");

			// 토큰 만료 대기
			Thread.sleep(2000);

			// when
			boolean isValid = shortLivedProvider.validateToken(token);

			// then
			assertThat(isValid).isFalse();
		}
	}

	@Nested
	@DisplayName("토큰 파싱")
	class ParseClaims {

		@Test
		@DisplayName("유효한 토큰 파싱 성공")
		void parseClaims_ValidToken_Success() {
			// given
			Long userId = 1L;
			String email = "test@example.com";
			String oauthProvider = "NAVER";
			String token = jwtTokenProvider.createAccessToken(userId, email, oauthProvider);

			// when
			Claims claims = jwtTokenProvider.parseClaims(token);

			// then
			assertThat(claims).isNotNull();
			assertThat(claims.getSubject()).isEqualTo(userId.toString());
			assertThat(claims.get("email", String.class)).isEqualTo(email);
			assertThat(claims.get("oauthProvider", String.class)).isEqualTo(oauthProvider);
			assertThat(claims.get("type", String.class)).isEqualTo("access");
		}

		@Test
		@DisplayName("잘못된 형식의 토큰 파싱 시 예외 발생")
		void parseClaims_MalformedToken_ThrowsException() {
			// given
			String malformedToken = "invalid.token.format";

			// when & then
			assertThatThrownBy(() -> jwtTokenProvider.parseClaims(malformedToken))
				.isInstanceOf(MalformedJwtException.class);
		}

		@Test
		@DisplayName("잘못된 서명의 토큰 파싱 시 예외 발생")
		void parseClaims_InvalidSignature_ThrowsException() {
			// given
			JwtTokenProvider anotherProvider = new JwtTokenProvider(
				"different-secret-key-for-jwt-token-generation-must-be-long-enough",
				3600L,
				604800L
			);
			String token = anotherProvider.createAccessToken(1L, "test@example.com", "NAVER");

			// when & then
			assertThatThrownBy(() -> jwtTokenProvider.parseClaims(token))
				.isInstanceOf(SignatureException.class);
		}

		@Test
		@DisplayName("만료된 토큰 파싱 시 예외 발생")
		void parseClaims_ExpiredToken_ThrowsException() throws InterruptedException {
			// given
			JwtTokenProvider shortLivedProvider = new JwtTokenProvider(
				SECRET,
				1L, // 1초
				604800L
			);
			String token = shortLivedProvider.createAccessToken(1L, "test@example.com", "NAVER");

			// 토큰 만료 대기
			Thread.sleep(2000);

			// when & then
			assertThatThrownBy(() -> shortLivedProvider.parseClaims(token))
				.isInstanceOf(ExpiredJwtException.class);
		}
	}

	@Nested
	@DisplayName("사용자 ID 추출")
	class GetUserId {

		@Test
		@DisplayName("Access Token에서 사용자 ID 추출 성공")
		void getUserId_FromAccessToken_Success() {
			// given
			Long userId = 123L;
			String token = jwtTokenProvider.createAccessToken(userId, "test@example.com", "NAVER");

			// when
			Long extractedUserId = jwtTokenProvider.getUserId(token);

			// then
			assertThat(extractedUserId).isEqualTo(userId);
		}

		@Test
		@DisplayName("Refresh Token에서 사용자 ID 추출 성공")
		void getUserId_FromRefreshToken_Success() {
			// given
			Long userId = 456L;
			String token = jwtTokenProvider.createRefreshToken(userId);

			// when
			Long extractedUserId = jwtTokenProvider.getUserId(token);

			// then
			assertThat(extractedUserId).isEqualTo(userId);
		}

		@Test
		@DisplayName("Temp Token에서 사용자 ID 추출 성공")
		void getUserId_FromTempToken_Success() {
			// given
			Long userId = 789L;
			String token = jwtTokenProvider.createTempToken(userId, "temp@example.com", "KAKAO");

			// when
			Long extractedUserId = jwtTokenProvider.getUserId(token);

			// then
			assertThat(extractedUserId).isEqualTo(userId);
		}
	}

	@Nested
	@DisplayName("토큰 유효 시간 조회")
	class GetTokenValidityInSeconds {

		@Test
		@DisplayName("Access Token 유효 시간 조회")
		void getAccessTokenValidityInSeconds_ReturnsCorrectValue() {
			// when
			long validity = jwtTokenProvider.getAccessTokenValidityInSeconds();

			// then
			assertThat(validity).isEqualTo(ACCESS_TOKEN_VALIDITY);
		}

		@Test
		@DisplayName("Refresh Token 유효 시간 조회")
		void getRefreshTokenValidityInSeconds_ReturnsCorrectValue() {
			// when
			long validity = jwtTokenProvider.getRefreshTokenValidityInSeconds();

			// then
			assertThat(validity).isEqualTo(REFRESH_TOKEN_VALIDITY);
		}
	}

	@Nested
	@DisplayName("토큰 타입 검증")
	class TokenTypeValidation {

		@Test
		@DisplayName("Access Token의 type 클레임은 'access'")
		void accessToken_TypeClaimIsAccess() {
			// given
			String token = jwtTokenProvider.createAccessToken(1L, "test@example.com", "NAVER");

			// when
			Claims claims = jwtTokenProvider.parseClaims(token);

			// then
			assertThat(claims.get("type", String.class)).isEqualTo("access");
		}

		@Test
		@DisplayName("Refresh Token의 type 클레임은 'refresh'")
		void refreshToken_TypeClaimIsRefresh() {
			// given
			String token = jwtTokenProvider.createRefreshToken(1L);

			// when
			Claims claims = jwtTokenProvider.parseClaims(token);

			// then
			assertThat(claims.get("type", String.class)).isEqualTo("refresh");
		}

		@Test
		@DisplayName("Temp Token의 type 클레임은 'temp'")
		void tempToken_TypeClaimIsTemp() {
			// given
			String token = jwtTokenProvider.createTempToken(1L, "test@example.com", "NAVER");

			// when
			Claims claims = jwtTokenProvider.parseClaims(token);

			// then
			assertThat(claims.get("type", String.class)).isEqualTo("temp");
		}
	}
}
