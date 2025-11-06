package com.example.adventcalendar.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OAuthDto {

	// Naver OAuth Response
	public record NaverOAuthResponse(
		NaverResponse response
	) {
		public record NaverResponse(
			String id,
			String email,
			String name,
			@JsonProperty("profile_image")
			String profileImage
		) {}
	}

	// Kakao OAuth Response
	public record KakaoOAuthResponse(
		Long id,
		@JsonProperty("kakao_account")
		KakaoAccount kakaoAccount
	) {
		public record KakaoAccount(
			KakaoProfile profile,
			String email
		) {
			public record KakaoProfile(
				String nickname,
				@JsonProperty("profile_image_url")
				String profileImageUrl
			) {}
		}
	}
}
