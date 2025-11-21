package com.example.adventcalendar.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.example.adventcalendar.dto.OAuthDto;
import com.example.adventcalendar.service.OAuth2Service.OAuthUserInfo;

@ExtendWith(MockitoExtension.class)
@DisplayName("OAuth2Service 단위 테스트")
class OAuth2ServiceTest {

	@Mock
	private RestTemplate restTemplate;

	@Spy
	@InjectMocks
	private OAuth2Service oAuth2Service;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(oAuth2Service, "restTemplate", restTemplate);
		ReflectionTestUtils.setField(oAuth2Service, "naverClientId", "naver-client-id");
		ReflectionTestUtils.setField(oAuth2Service, "naverClientSecret", "naver-client-secret");
		ReflectionTestUtils.setField(oAuth2Service, "naverRedirectUri", "http://localhost:8080/api/auth/oauth/naver/callback");
		ReflectionTestUtils.setField(oAuth2Service, "kakaoClientId", "kakao-client-id");
		ReflectionTestUtils.setField(oAuth2Service, "kakaoClientSecret", "kakao-client-secret");
		ReflectionTestUtils.setField(oAuth2Service, "kakaoRedirectUri", "http://localhost:8080/api/auth/oauth/kakao/callback");
	}

	@Nested
	@DisplayName("네이버 OAuth 인증")
	class AuthenticateNaver {

		@Test
		@DisplayName("네이버 인증 성공")
		void authenticateNaver_Success() {
			// given
			String code = "auth-code";
			String state = "state-value";
			String accessToken = "naver-access-token";

			// Mock Access Token 응답
			Map<String, Object> tokenResponse = new HashMap<>();
			tokenResponse.put("access_token", accessToken);
			tokenResponse.put("token_type", "bearer");
			tokenResponse.put("expires_in", 3600);

			given(restTemplate.postForEntity(
				anyString(),
				any(HttpEntity.class),
				eq(Map.class)
			)).willReturn(ResponseEntity.ok(tokenResponse));

			// Mock 사용자 정보 응답
			OAuthDto.NaverOAuthResponse.NaverResponse naverResponse =
				new OAuthDto.NaverOAuthResponse.NaverResponse(
					"naver123",
					"test@naver.com",
					"테스트",
					"http://example.com/profile.jpg"
				);

			OAuthDto.NaverOAuthResponse userInfoResponse =
				new OAuthDto.NaverOAuthResponse(naverResponse);

			given(restTemplate.exchange(
				anyString(),
				eq(HttpMethod.GET),
				any(HttpEntity.class),
				eq(OAuthDto.NaverOAuthResponse.class)
			)).willReturn(ResponseEntity.ok(userInfoResponse));

			// when
			OAuthUserInfo result = oAuth2Service.authenticateNaver(code, state);

			// then
			assertThat(result).isNotNull();
			assertThat(result.getOauthProvider()).isEqualTo("NAVER");
			assertThat(result.getOauthId()).isEqualTo("naver123");
			assertThat(result.getEmail()).isEqualTo("test@naver.com");
			assertThat(result.getName()).isEqualTo("테스트");
			assertThat(result.getProfileImageUrl()).isEqualTo("http://example.com/profile.jpg");

			verify(restTemplate).postForEntity(anyString(), any(HttpEntity.class), eq(Map.class));
			verify(restTemplate).exchange(
				anyString(),
				eq(HttpMethod.GET),
				any(HttpEntity.class),
				eq(OAuthDto.NaverOAuthResponse.class)
			);
		}

		@Test
		@DisplayName("네이버 Access Token 발급 실패 - access_token 없음")
		void authenticateNaver_TokenIssueFailed_NoAccessToken() {
			// given
			String code = "auth-code";
			String state = "state-value";

			Map<String, Object> tokenResponse = new HashMap<>();
			tokenResponse.put("error", "invalid_request");

			given(restTemplate.postForEntity(
				anyString(),
				any(HttpEntity.class),
				eq(Map.class)
			)).willReturn(ResponseEntity.ok(tokenResponse));

			// when & then
			assertThatThrownBy(() -> oAuth2Service.authenticateNaver(code, state))
				.isInstanceOf(RuntimeException.class)
				.hasMessage("네이버 Access Token 발급 실패");
		}

		@Test
		@DisplayName("네이버 Access Token 발급 실패 - null 응답")
		void authenticateNaver_TokenIssueFailed_NullResponse() {
			// given
			String code = "auth-code";
			String state = "state-value";

			given(restTemplate.postForEntity(
				anyString(),
				any(HttpEntity.class),
				eq(Map.class)
			)).willReturn(ResponseEntity.ok(null));

			// when & then
			assertThatThrownBy(() -> oAuth2Service.authenticateNaver(code, state))
				.isInstanceOf(RuntimeException.class)
				.hasMessage("네이버 Access Token 발급 실패");
		}

		@Test
		@DisplayName("네이버 사용자 정보 조회 실패 - null 응답")
		void authenticateNaver_UserInfoFailed_NullResponse() {
			// given
			String code = "auth-code";
			String state = "state-value";
			String accessToken = "naver-access-token";

			Map<String, Object> tokenResponse = new HashMap<>();
			tokenResponse.put("access_token", accessToken);

			given(restTemplate.postForEntity(
				anyString(),
				any(HttpEntity.class),
				eq(Map.class)
			)).willReturn(ResponseEntity.ok(tokenResponse));

			given(restTemplate.exchange(
				anyString(),
				eq(HttpMethod.GET),
				any(HttpEntity.class),
				eq(OAuthDto.NaverOAuthResponse.class)
			)).willReturn(ResponseEntity.ok(null));

			// when & then
			assertThatThrownBy(() -> oAuth2Service.authenticateNaver(code, state))
				.isInstanceOf(RuntimeException.class)
				.hasMessage("네이버 사용자 정보 조회 실패");
		}

		@Test
		@DisplayName("네이버 사용자 정보 조회 실패 - response 필드 없음")
		void authenticateNaver_UserInfoFailed_NoResponseField() {
			// given
			String code = "auth-code";
			String state = "state-value";
			String accessToken = "naver-access-token";

			Map<String, Object> tokenResponse = new HashMap<>();
			tokenResponse.put("access_token", accessToken);

			given(restTemplate.postForEntity(
				anyString(),
				any(HttpEntity.class),
				eq(Map.class)
			)).willReturn(ResponseEntity.ok(tokenResponse));

			OAuthDto.NaverOAuthResponse userInfoResponse =
				new OAuthDto.NaverOAuthResponse(null);

			given(restTemplate.exchange(
				anyString(),
				eq(HttpMethod.GET),
				any(HttpEntity.class),
				eq(OAuthDto.NaverOAuthResponse.class)
			)).willReturn(ResponseEntity.ok(userInfoResponse));

			// when & then
			assertThatThrownBy(() -> oAuth2Service.authenticateNaver(code, state))
				.isInstanceOf(RuntimeException.class)
				.hasMessage("네이버 사용자 정보 조회 실패");
		}

		@Test
		@DisplayName("네이버 API 호출 중 네트워크 오류")
		void authenticateNaver_NetworkError() {
			// given
			String code = "auth-code";
			String state = "state-value";

			given(restTemplate.postForEntity(
				anyString(),
				any(HttpEntity.class),
				eq(Map.class)
			)).willThrow(new RestClientException("Network error"));

			// when & then
			assertThatThrownBy(() -> oAuth2Service.authenticateNaver(code, state))
				.isInstanceOf(RestClientException.class);
		}
	}

	@Nested
	@DisplayName("카카오 OAuth 인증")
	class AuthenticateKakao {

		@Test
		@DisplayName("카카오 인증 성공")
		void authenticateKakao_Success() {
			// given
			String code = "auth-code";
			String accessToken = "kakao-access-token";

			// Mock Access Token 응답
			Map<String, Object> tokenResponse = new HashMap<>();
			tokenResponse.put("access_token", accessToken);
			tokenResponse.put("token_type", "bearer");
			tokenResponse.put("expires_in", 3600);

			given(restTemplate.postForEntity(
				anyString(),
				any(HttpEntity.class),
				eq(Map.class)
			)).willReturn(ResponseEntity.ok(tokenResponse));

			// Mock 사용자 정보 응답
			OAuthDto.KakaoOAuthResponse.KakaoAccount.KakaoProfile profile =
				new OAuthDto.KakaoOAuthResponse.KakaoAccount.KakaoProfile(
					"카카오",
					"http://example.com/kakao.jpg"
				);

			OAuthDto.KakaoOAuthResponse.KakaoAccount account =
				new OAuthDto.KakaoOAuthResponse.KakaoAccount(profile, "test@kakao.com");

			OAuthDto.KakaoOAuthResponse userInfoResponse =
				new OAuthDto.KakaoOAuthResponse(123456789L, account);

			given(restTemplate.exchange(
				anyString(),
				eq(HttpMethod.GET),
				any(HttpEntity.class),
				eq(OAuthDto.KakaoOAuthResponse.class)
			)).willReturn(ResponseEntity.ok(userInfoResponse));

			// when
			OAuthUserInfo result = oAuth2Service.authenticateKakao(code);

			// then
			assertThat(result).isNotNull();
			assertThat(result.getOauthProvider()).isEqualTo("KAKAO");
			assertThat(result.getOauthId()).isEqualTo("123456789");
			assertThat(result.getEmail()).isEqualTo("test@kakao.com");
			assertThat(result.getName()).isEqualTo("카카오");
			assertThat(result.getProfileImageUrl()).isEqualTo("http://example.com/kakao.jpg");

			verify(restTemplate).postForEntity(anyString(), any(HttpEntity.class), eq(Map.class));
			verify(restTemplate).exchange(
				anyString(),
				eq(HttpMethod.GET),
				any(HttpEntity.class),
				eq(OAuthDto.KakaoOAuthResponse.class)
			);
		}

		@Test
		@DisplayName("카카오 Access Token 발급 실패 - access_token 없음")
		void authenticateKakao_TokenIssueFailed_NoAccessToken() {
			// given
			String code = "auth-code";

			Map<String, Object> tokenResponse = new HashMap<>();
			tokenResponse.put("error", "invalid_grant");

			given(restTemplate.postForEntity(
				anyString(),
				any(HttpEntity.class),
				eq(Map.class)
			)).willReturn(ResponseEntity.ok(tokenResponse));

			// when & then
			assertThatThrownBy(() -> oAuth2Service.authenticateKakao(code))
				.isInstanceOf(RuntimeException.class)
				.hasMessage("카카오 Access Token 발급 실패");
		}

		@Test
		@DisplayName("카카오 Access Token 발급 실패 - null 응답")
		void authenticateKakao_TokenIssueFailed_NullResponse() {
			// given
			String code = "auth-code";

			given(restTemplate.postForEntity(
				anyString(),
				any(HttpEntity.class),
				eq(Map.class)
			)).willReturn(ResponseEntity.ok(null));

			// when & then
			assertThatThrownBy(() -> oAuth2Service.authenticateKakao(code))
				.isInstanceOf(RuntimeException.class)
				.hasMessage("카카오 Access Token 발급 실패");
		}

		@Test
		@DisplayName("카카오 사용자 정보 조회 실패 - null 응답")
		void authenticateKakao_UserInfoFailed_NullResponse() {
			// given
			String code = "auth-code";
			String accessToken = "kakao-access-token";

			Map<String, Object> tokenResponse = new HashMap<>();
			tokenResponse.put("access_token", accessToken);

			given(restTemplate.postForEntity(
				anyString(),
				any(HttpEntity.class),
				eq(Map.class)
			)).willReturn(ResponseEntity.ok(tokenResponse));

			given(restTemplate.exchange(
				anyString(),
				eq(HttpMethod.GET),
				any(HttpEntity.class),
				eq(OAuthDto.KakaoOAuthResponse.class)
			)).willReturn(ResponseEntity.ok(null));

			// when & then
			assertThatThrownBy(() -> oAuth2Service.authenticateKakao(code))
				.isInstanceOf(RuntimeException.class)
				.hasMessage("카카오 사용자 정보 조회 실패");
		}

		@Test
		@DisplayName("카카오 API 호출 중 네트워크 오류")
		void authenticateKakao_NetworkError() {
			// given
			String code = "auth-code";

			given(restTemplate.postForEntity(
				anyString(),
				any(HttpEntity.class),
				eq(Map.class)
			)).willThrow(new RestClientException("Network timeout"));

			// when & then
			assertThatThrownBy(() -> oAuth2Service.authenticateKakao(code))
				.isInstanceOf(RestClientException.class);
		}

		@Test
		@DisplayName("카카오 사용자 ID가 Long 타입으로 정상 처리")
		void authenticateKakao_UserIdAsLong_Success() {
			// given
			String code = "auth-code";
			String accessToken = "kakao-access-token";

			Map<String, Object> tokenResponse = new HashMap<>();
			tokenResponse.put("access_token", accessToken);

			given(restTemplate.postForEntity(
				anyString(),
				any(HttpEntity.class),
				eq(Map.class)
			)).willReturn(ResponseEntity.ok(tokenResponse));

			OAuthDto.KakaoOAuthResponse.KakaoAccount.KakaoProfile profile =
				new OAuthDto.KakaoOAuthResponse.KakaoAccount.KakaoProfile(
					"닉네임",
					"http://image.url"
				);

			OAuthDto.KakaoOAuthResponse.KakaoAccount account =
				new OAuthDto.KakaoOAuthResponse.KakaoAccount(profile, "email@test.com");

			Long userId = 9876543210L;
			OAuthDto.KakaoOAuthResponse userInfoResponse =
				new OAuthDto.KakaoOAuthResponse(userId, account);

			given(restTemplate.exchange(
				anyString(),
				eq(HttpMethod.GET),
				any(HttpEntity.class),
				eq(OAuthDto.KakaoOAuthResponse.class)
			)).willReturn(ResponseEntity.ok(userInfoResponse));

			// when
			OAuthUserInfo result = oAuth2Service.authenticateKakao(code);

			// then
			assertThat(result).isNotNull();
			assertThat(result.getOauthId()).isEqualTo("9876543210");
		}
	}

	@Nested
	@DisplayName("OAuthUserInfo 빌더 테스트")
	class OAuthUserInfoBuilder {

		@Test
		@DisplayName("OAuthUserInfo 객체 생성 성공")
		void buildOAuthUserInfo_Success() {
			// when
			OAuthUserInfo userInfo = OAuthUserInfo.builder()
				.oauthProvider("NAVER")
				.oauthId("naver123")
				.email("test@example.com")
				.name("테스트")
				.profileImageUrl("http://example.com/image.jpg")
				.build();

			// then
			assertThat(userInfo.getOauthProvider()).isEqualTo("NAVER");
			assertThat(userInfo.getOauthId()).isEqualTo("naver123");
			assertThat(userInfo.getEmail()).isEqualTo("test@example.com");
			assertThat(userInfo.getName()).isEqualTo("테스트");
			assertThat(userInfo.getProfileImageUrl()).isEqualTo("http://example.com/image.jpg");
		}

		@Test
		@DisplayName("OAuthUserInfo null 값 허용")
		void buildOAuthUserInfo_WithNullValues() {
			// when
			OAuthUserInfo userInfo = OAuthUserInfo.builder()
				.oauthProvider("KAKAO")
				.oauthId("kakao123")
				.email(null)
				.name("이름만")
				.profileImageUrl(null)
				.build();

			// then
			assertThat(userInfo.getOauthProvider()).isEqualTo("KAKAO");
			assertThat(userInfo.getOauthId()).isEqualTo("kakao123");
			assertThat(userInfo.getEmail()).isNull();
			assertThat(userInfo.getName()).isEqualTo("이름만");
			assertThat(userInfo.getProfileImageUrl()).isNull();
		}
	}

	/*@Nested
	@DisplayName("Access Token 요청 파라미터 검증")
	class AccessTokenRequestValidation {

		@Test
		@DisplayName("네이버 토큰 요청에 필수 파라미터 포함 확인")
		void naverTokenRequest_ContainsRequiredParameters() {
			// given
			String code = "test-code";
			String state = "test-state";

			Map<String, Object> tokenResponse = new HashMap<>();
			tokenResponse.put("access_token", "token");

			given(restTemplate.postForEntity(
				anyString(),
				any(HttpEntity.class),
				eq(Map.class)
			)).willAnswer(invocation -> {
				String url = invocation.getArgument(0);

				// URL에 필수 파라미터가 포함되어 있는지 확인
				assertThat(url).contains("grant_type=authorization_code");
				assertThat(url).contains("client_id=naver-client-id");
				assertThat(url).contains("client_secret=naver-client-secret");
				assertThat(url).contains("code=" + code);
				assertThat(url).contains("state=" + state);

				return ResponseEntity.ok(tokenResponse);
			});

			OAuthDto.NaverOAuthResponse.NaverResponse naverResponse =
				new OAuthDto.NaverOAuthResponse.NaverResponse(
					"id", "email", "name", "image"
				);
			OAuthDto.NaverOAuthResponse userInfo =
				new OAuthDto.NaverOAuthResponse(naverResponse);

			given(restTemplate.exchange(
				anyString(),
				eq(HttpMethod.GET),
				any(HttpEntity.class),
				eq(OAuthDto.NaverOAuthResponse.class)
			)).willReturn(ResponseEntity.ok(userInfo));

			// when
			oAuth2Service.authenticateNaver(code, state);

			// then
			verify(restTemplate).postForEntity(anyString(), any(HttpEntity.class), eq(Map.class));
		}

		@Test
		@DisplayName("카카오 토큰 요청에 필수 파라미터 포함 확인")
		void kakaoTokenRequest_ContainsRequiredParameters() {
			// given
			String code = "test-code";

			Map<String, Object> tokenResponse = new HashMap<>();
			tokenResponse.put("access_token", "token");

			given(restTemplate.postForEntity(
				anyString(),
				any(HttpEntity.class),
				eq(Map.class)
			)).willAnswer(invocation -> {
				String url = invocation.getArgument(0);

				// URL에 필수 파라미터가 포함되어 있는지 확인
				assertThat(url).contains("grant_type=authorization_code");
				assertThat(url).contains("client_id=kakao-client-id");
				assertThat(url).contains("client_secret=kakao-client-secret");
				assertThat(url).contains("code=" + code);
				assertThat(url).contains("redirect_uri=");

				return ResponseEntity.ok(tokenResponse);
			});

			OAuthDto.KakaoOAuthResponse.KakaoAccount.KakaoProfile profile =
				new OAuthDto.KakaoOAuthResponse.KakaoAccount.KakaoProfile("name", "image");
			OAuthDto.KakaoOAuthResponse.KakaoAccount account =
				new OAuthDto.KakaoOAuthResponse.KakaoAccount(profile, "email");
			OAuthDto.KakaoOAuthResponse userInfo =
				new OAuthDto.KakaoOAuthResponse(123L, account);

			given(restTemplate.exchange(
				anyString(),
				eq(HttpMethod.GET),
				any(HttpEntity.class),
				eq(OAuthDto.KakaoOAuthResponse.class)
			)).willReturn(ResponseEntity.ok(userInfo));

			// when
			oAuth2Service.authenticateKakao(code);

			// then
			verify(restTemplate).postForEntity(anyString(), any(HttpEntity.class), eq(Map.class));
		}
	}*/
}
