package com.example.adventcalendar.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

@Configuration
public class OAuth2Config {

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

	@Bean
	public ClientRegistrationRepository clientRegistrationRepository() {
		return new InMemoryClientRegistrationRepository(
			naverClientRegistration(),
			kakaoClientRegistration()
		);
	}

	private ClientRegistration naverClientRegistration() {
		return ClientRegistration.withRegistrationId("naver")
			.clientId(naverClientId)
			.clientSecret(naverClientSecret)
			.redirectUri(naverRedirectUri)
			.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
			.scope("name", "email", "profile_image")
			.authorizationUri("https://nid.naver.com/oauth2.0/authorize")
			.tokenUri("https://nid.naver.com/oauth2.0/token")
			.userInfoUri("https://openapi.naver.com/v1/nid/me")
			.userNameAttributeName("response")
			.clientName("Naver")
			.build();
	}

	private ClientRegistration kakaoClientRegistration() {
		return ClientRegistration.withRegistrationId("kakao")
			.clientId(kakaoClientId)
			.clientSecret(kakaoClientSecret)
			.redirectUri(kakaoRedirectUri)
			.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
			.scope("profile_nickname", "profile_image", "account_email")
			.authorizationUri("https://kauth.kakao.com/oauth/authorize")
			.tokenUri("https://kauth.kakao.com/oauth/token")
			.userInfoUri("https://kapi.kakao.com/v2/user/me")
			.userNameAttributeName("id")
			.clientName("Kakao")
			.build();
	}
}
