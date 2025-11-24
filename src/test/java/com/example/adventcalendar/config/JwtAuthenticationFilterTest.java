package com.example.adventcalendar.config;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter 단위 테스트")
class JwtAuthenticationFilterTest {

	@Mock
	private JwtTokenProvider jwtTokenProvider;

	@InjectMocks
	private JwtAuthenticationFilter jwtAuthenticationFilter;

	private MockHttpServletRequest request;
	private MockHttpServletResponse response;
	private MockFilterChain filterChain;

	@BeforeEach
	void setUp() {
		request = new MockHttpServletRequest();
		response = new MockHttpServletResponse();
		filterChain = new MockFilterChain();
		SecurityContextHolder.clearContext();
	}

	@Nested
	@DisplayName("Authorization 헤더에서 토큰 추출")
	class ExtractTokenFromHeader {

		@Test
		@DisplayName("Bearer 토큰으로 인증 성공")
		void doFilterInternal_BearerToken_Success() throws ServletException, IOException {
			// given
			String token = "valid-access-token";
			Long userId = 1L;

			request.addHeader("Authorization", "Bearer " + token);

			Claims claims = Jwts.claims()
				.setSubject(userId.toString())
				.add("type", "access")
				.build();

			given(jwtTokenProvider.validateToken(token)).willReturn(true);
			given(jwtTokenProvider.parseClaims(token)).willReturn(claims);

			// when
			jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

			// then
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			assertThat(authentication).isNotNull();
			assertThat(authentication.getPrincipal()).isEqualTo(userId);
			assertThat(authentication.getAuthorities()).hasSize(1);
			assertThat(authentication.getAuthorities().iterator().next().getAuthority())
				.isEqualTo("ROLE_USER");

			verify(jwtTokenProvider).validateToken(token);
			verify(jwtTokenProvider).parseClaims(token);
		}

		@Test
		@DisplayName("Bearer 접두사 없는 토큰은 무시")
		void doFilterInternal_NoBearerPrefix_Ignored() throws ServletException, IOException {
			// given
			request.addHeader("Authorization", "invalid-token");

			// when
			jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

			// then
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			assertThat(authentication).isNull();

			verify(jwtTokenProvider, never()).validateToken(anyString());
		}

		@Test
		@DisplayName("Authorization 헤더 없으면 인증 없이 진행")
		void doFilterInternal_NoAuthorizationHeader_Continues() throws ServletException, IOException {
			// when
			jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

			// then
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			assertThat(authentication).isNull();

			verify(jwtTokenProvider, never()).validateToken(anyString());
		}
	}

	@Nested
	@DisplayName("Cookie에서 토큰 추출")
	class ExtractTokenFromCookie {

		@Test
		@DisplayName("accessToken 쿠키로 인증 성공")
		void doFilterInternal_AccessTokenCookie_Success() throws ServletException, IOException {
			// given
			String token = "valid-access-token";
			Long userId = 1L;

			Cookie accessTokenCookie = new Cookie("accessToken", token);
			request.setCookies(accessTokenCookie);

			Claims claims = Jwts.claims()
				.setSubject(userId.toString())
				.add("type", "access")
				.build();

			given(jwtTokenProvider.validateToken(token)).willReturn(true);
			given(jwtTokenProvider.parseClaims(token)).willReturn(claims);

			// when
			jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

			// then
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			assertThat(authentication).isNotNull();
			assertThat(authentication.getPrincipal()).isEqualTo(userId);
		}

		@Test
		@DisplayName("tempToken 쿠키로 인증 성공")
		void doFilterInternal_TempTokenCookie_Success() throws ServletException, IOException {
			// given
			String token = "valid-temp-token";
			Long userId = 2L;

			Cookie tempTokenCookie = new Cookie("tempToken", token);
			request.setCookies(tempTokenCookie);

			Claims claims = Jwts.claims()
				.setSubject(userId.toString())
				.add("type", "temp")
				.build();

			given(jwtTokenProvider.validateToken(token)).willReturn(true);
			given(jwtTokenProvider.parseClaims(token)).willReturn(claims);

			// when
			jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

			// then
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			assertThat(authentication).isNotNull();
			assertThat(authentication.getPrincipal()).isEqualTo(userId);
		}

		@Test
		@DisplayName("refreshToken 쿠키는 인증에 사용되지 않음")
		void doFilterInternal_RefreshTokenCookie_NotUsedForAuthentication() throws ServletException, IOException {
			// given
			String token = "valid-refresh-token";

			Cookie refreshTokenCookie = new Cookie("refreshToken", token);
			request.setCookies(refreshTokenCookie);

			// when
			jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

			// then
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			assertThat(authentication).isNull();

			// refreshToken 쿠키는 무시되므로 validateToken이 호출되지 않음을 검증
			verify(jwtTokenProvider, never()).validateToken(anyString());
			verify(jwtTokenProvider, never()).parseClaims(anyString());
		}

		@Test
		@DisplayName("여러 쿠키 중 accessToken 우선 사용")
		void doFilterInternal_MultipleCookies_UsesAccessToken() throws ServletException, IOException {
			// given
			String token = "valid-access-token";
			Long userId = 1L;

			Cookie[] cookies = {
				new Cookie("otherCookie", "value"),
				new Cookie("accessToken", token),
				new Cookie("anotherCookie", "value2")
			};
			request.setCookies(cookies);

			Claims claims = Jwts.claims()
				.setSubject(userId.toString())
				.add("type", "access")
				.build();

			given(jwtTokenProvider.validateToken(token)).willReturn(true);
			given(jwtTokenProvider.parseClaims(token)).willReturn(claims);

			// when
			jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

			// then
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			assertThat(authentication).isNotNull();
			assertThat(authentication.getPrincipal()).isEqualTo(userId);
		}
	}

	@Nested
	@DisplayName("토큰 우선순위")
	class TokenPriority {

		@Test
		@DisplayName("Authorization 헤더가 쿠키보다 우선")
		void doFilterInternal_HeaderOverCookie() throws ServletException, IOException {
			// given
			String headerToken = "header-token";
			String cookieToken = "cookie-token";
			Long userId = 1L;

			request.addHeader("Authorization", "Bearer " + headerToken);
			Cookie accessTokenCookie = new Cookie("accessToken", cookieToken);
			request.setCookies(accessTokenCookie);

			Claims claims = Jwts.claims()
				.setSubject(userId.toString())
				.add("type", "access")
				.build();

			given(jwtTokenProvider.validateToken(headerToken)).willReturn(true);
			given(jwtTokenProvider.parseClaims(headerToken)).willReturn(claims);

			// when
			jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

			// then
			verify(jwtTokenProvider).validateToken(headerToken);
			verify(jwtTokenProvider, never()).validateToken(cookieToken);
		}
	}

	@Nested
	@DisplayName("유효하지 않은 토큰 처리")
	class InvalidTokenHandling {

		@Test
		@DisplayName("유효하지 않은 토큰은 인증 없이 진행")
		void doFilterInternal_InvalidToken_ContinuesWithoutAuth() throws ServletException, IOException {
			// given
			String token = "invalid-token";
			request.addHeader("Authorization", "Bearer " + token);

			given(jwtTokenProvider.validateToken(token)).willReturn(false);

			// when
			jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

			// then
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			assertThat(authentication).isNull();

			verify(jwtTokenProvider).validateToken(token);
			verify(jwtTokenProvider, never()).parseClaims(anyString());
		}

		@Test
		@DisplayName("만료된 토큰 처리")
		void doFilterInternal_ExpiredToken_ContinuesWithoutAuth() throws ServletException, IOException {
			// given
			String token = "expired-token";
			request.addHeader("Authorization", "Bearer " + token);

			given(jwtTokenProvider.validateToken(token)).willReturn(false);

			// when
			jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

			// then
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			assertThat(authentication).isNull();
		}

		@Test
		@DisplayName("잘못된 형식의 토큰 처리")
		void doFilterInternal_MalformedToken_ContinuesWithoutAuth() throws ServletException, IOException {
			// given
			String token = "malformed-token";
			request.addHeader("Authorization", "Bearer " + token);

			given(jwtTokenProvider.validateToken(token)).willThrow(new MalformedJwtException("Malformed"));

			// when
			jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

			// then
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			assertThat(authentication).isNull();
		}

		@Test
		@DisplayName("잘못된 서명 토큰 처리")
		void doFilterInternal_InvalidSignature_ContinuesWithoutAuth() throws ServletException, IOException {
			// given
			String token = "invalid-signature-token";
			request.addHeader("Authorization", "Bearer " + token);

			given(jwtTokenProvider.validateToken(token)).willThrow(new SignatureException("Invalid signature"));

			// when
			jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

			// then
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			assertThat(authentication).isNull();
		}
	}

	@Nested
	@DisplayName("토큰 타입 검증")
	class TokenTypeValidation {

		@Test
		@DisplayName("access 타입 토큰은 인증 성공")
		void doFilterInternal_AccessType_Success() throws ServletException, IOException {
			// given
			String token = "access-token";
			Long userId = 1L;

			request.addHeader("Authorization", "Bearer " + token);

			Claims claims = Jwts.claims()
				.setSubject(userId.toString())
				.add("type", "access")
				.build();

			given(jwtTokenProvider.validateToken(token)).willReturn(true);
			given(jwtTokenProvider.parseClaims(token)).willReturn(claims);

			// when
			jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

			// then
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			assertThat(authentication).isNotNull();
		}

		@Test
		@DisplayName("temp 타입 토큰은 인증 성공")
		void doFilterInternal_TempType_Success() throws ServletException, IOException {
			// given
			String token = "temp-token";
			Long userId = 1L;

			request.addHeader("Authorization", "Bearer " + token);

			Claims claims = Jwts.claims()
				.setSubject(userId.toString())
				.add("type", "temp")
				.build();

			given(jwtTokenProvider.validateToken(token)).willReturn(true);
			given(jwtTokenProvider.parseClaims(token)).willReturn(claims);

			// when
			jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

			// then
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			assertThat(authentication).isNotNull();
		}

		@Test
		@DisplayName("refresh 타입 토큰은 인증 실패")
		void doFilterInternal_RefreshType_NoAuthentication() throws ServletException, IOException {
			// given
			String token = "refresh-token";

			request.addHeader("Authorization", "Bearer " + token);

			Claims claims = Jwts.claims()
				.setSubject("1")
				.add("type", "refresh")
				.build();

			given(jwtTokenProvider.validateToken(token)).willReturn(true);
			given(jwtTokenProvider.parseClaims(token)).willReturn(claims);

			// when
			jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

			// then
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			assertThat(authentication).isNull();
		}

		@Test
		@DisplayName("type 클레임이 없는 토큰은 인증 실패")
		void doFilterInternal_NoTypeClaim_NoAuthentication() throws ServletException, IOException {
			// given
			String token = "no-type-token";

			request.addHeader("Authorization", "Bearer " + token);

			Claims claims = Jwts.claims()
				.setSubject("1")
				.build();
			// type 클레임 없음

			given(jwtTokenProvider.validateToken(token)).willReturn(true);
			given(jwtTokenProvider.parseClaims(token)).willReturn(claims);

			// when
			jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

			// then
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			assertThat(authentication).isNull();
		}
	}

	@Nested
	@DisplayName("예외 처리")
	class ExceptionHandling {

		@Test
		@DisplayName("토큰 파싱 중 예외 발생해도 필터 체인 계속 진행")
		void doFilterInternal_ParsingException_ContinuesFilterChain() throws ServletException, IOException {
			// given
			String token = "token-causing-exception";
			request.addHeader("Authorization", "Bearer " + token);

			given(jwtTokenProvider.validateToken(token)).willReturn(true);
			given(jwtTokenProvider.parseClaims(token)).willThrow(new RuntimeException("Parsing error"));

			// when
			jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

			// then
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			assertThat(authentication).isNull();
		}

		@Test
		@DisplayName("검증 중 예외 발생해도 필터 체인 계속 진행")
		void doFilterInternal_ValidationException_ContinuesFilterChain() throws ServletException, IOException {
			// given
			String token = "token-causing-validation-error";
			request.addHeader("Authorization", "Bearer " + token);

			given(jwtTokenProvider.validateToken(token)).willThrow(new ExpiredJwtException(null, null, "Expired"));

			// when
			jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

			// then
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			assertThat(authentication).isNull();
		}
	}

	@Nested
	@DisplayName("사용자 ID 파싱")
	class UserIdParsing {

		@Test
		@DisplayName("Long 타입 사용자 ID 정상 파싱")
		void doFilterInternal_LongUserId_Success() throws ServletException, IOException {
			// given
			String token = "valid-token";
			Long userId = 123456789L;

			request.addHeader("Authorization", "Bearer " + token);

			Claims claims = Jwts.claims()
				.setSubject(userId.toString())
				.add("type", "access")
				.build();

			given(jwtTokenProvider.validateToken(token)).willReturn(true);
			given(jwtTokenProvider.parseClaims(token)).willReturn(claims);

			// when
			jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

			// then
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			assertThat(authentication).isNotNull();
			assertThat(authentication.getPrincipal()).isEqualTo(userId);
		}
	}
}
