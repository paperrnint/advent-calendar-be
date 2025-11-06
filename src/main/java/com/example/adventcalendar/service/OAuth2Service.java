package com.example.adventcalendar.service;

import com.example.adventcalendar.dto.OAuthDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2Service {

	private final RestTemplate restTemplate = new RestTemplate();

	@Value("${oauth2.naver.client-id}")
	private String naverClientId;

	@Value("${oauth2.naver.client-secret}")
	private String naverClientSecret;

	@Value("${oauth2.naver.redirect-uri}")
	private String naverRedirectUri;

	@Value("${oauth2.kakao.client-id}")
	private String kakaoClientId;

	@Value("${oauth2.kakao.client-secret}")
	private String kakaoClientSecret;

	@Value("${oauth2.kakao.redirect-uri}")
	private String kakaoRedirectUri;

	public OAuthUserInfo authenticateNaver(String code, String state) {
		String accessToken = getNaverAccessToken(code, state);

		return getNaverUserInfo(accessToken);
	}

	public OAuthUserInfo authenticateKakao(String code) {
		String accessToken = getKakaoAccessToken(code);

		return getKakaoUserInfo(accessToken);
	}

	private String getNaverAccessToken(String code, String state) {
		String tokenUrl = "https://nid.naver.com/oauth2.0/token";

		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("grant_type", "authorization_code");
		params.add("client_id", naverClientId);
		params.add("client_secret", naverClientSecret);
		params.add("code", code);
		params.add("state", state);

		HttpHeaders headers = new HttpHeaders();
		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

		ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);
		Map<String, Object> body = response.getBody();

		if (body == null || !body.containsKey("access_token")) {
			throw new RuntimeException("네이버 Access Token 발급 실패");
		}

		return (String) body.get("access_token");
	}

	private OAuthUserInfo getNaverUserInfo(String accessToken) {
		String userInfoUrl = "https://openapi.naver.com/v1/nid/me";

		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(accessToken);
		HttpEntity<String> request = new HttpEntity<>(headers);

		ResponseEntity<OAuthDto.NaverOAuthResponse> response = restTemplate.exchange(
			userInfoUrl,
			HttpMethod.GET,
			request,
			OAuthDto.NaverOAuthResponse.class
		);

		OAuthDto.NaverOAuthResponse naverResponse = response.getBody();
		if (naverResponse == null || naverResponse.response() == null) {
			throw new RuntimeException("네이버 사용자 정보 조회 실패");
		}

		OAuthDto.NaverOAuthResponse.NaverResponse userInfo = naverResponse.response();

		return OAuthUserInfo.builder()
			.oauthProvider("NAVER")
			.oauthId(userInfo.id())
			.email(userInfo.email())
			.name(userInfo.name())
			.profileImageUrl(userInfo.profileImage())
			.build();
	}

	private String getKakaoAccessToken(String code) {
		String tokenUrl = "https://kauth.kakao.com/oauth/token";

		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("grant_type", "authorization_code");
		params.add("client_id", kakaoClientId);
		params.add("client_secret", kakaoClientSecret);
		params.add("redirect_uri", kakaoRedirectUri);
		params.add("code", code);

		HttpHeaders headers = new HttpHeaders();
		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

		ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);
		Map<String, Object> body = response.getBody();

		if (body == null || !body.containsKey("access_token")) {
			throw new RuntimeException("카카오 Access Token 발급 실패");
		}

		return (String) body.get("access_token");
	}

	private OAuthUserInfo getKakaoUserInfo(String accessToken) {
		String userInfoUrl = "https://kapi.kakao.com/v2/user/me";

		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(accessToken);
		HttpEntity<String> request = new HttpEntity<>(headers);

		ResponseEntity<OAuthDto.KakaoOAuthResponse> response = restTemplate.exchange(
			userInfoUrl,
			HttpMethod.GET,
			request,
			OAuthDto.KakaoOAuthResponse.class
		);

		OAuthDto.KakaoOAuthResponse kakaoResponse = response.getBody();
		if (kakaoResponse == null) {
			throw new RuntimeException("카카오 사용자 정보 조회 실패");
		}

		OAuthDto.KakaoOAuthResponse.KakaoAccount account = kakaoResponse.kakaoAccount();
		OAuthDto.KakaoOAuthResponse.KakaoAccount.KakaoProfile profile = account.profile();

		return OAuthUserInfo.builder()
			.oauthProvider("KAKAO")
			.oauthId(kakaoResponse.id().toString())
			.email(account.email())
			.name(profile.nickname())
			.profileImageUrl(profile.profileImageUrl())
			.build();
	}

	@lombok.Getter
	@lombok.Builder
	public static class OAuthUserInfo {
		private String oauthProvider;
		private String oauthId;
		private String email;
		private String name;
		private String profileImageUrl;
	}
}
