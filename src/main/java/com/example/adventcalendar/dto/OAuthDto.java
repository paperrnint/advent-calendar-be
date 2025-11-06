package com.example.adventcalendar.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthDto {

	private Long id;
	private String email;
	private String name;
	private String profileImageUrl;
	private String oauthProvider;
	private String oauthId;

	// Naver OAuth Response
	@Getter
	@Setter
	public static class NaverOAuthResponse {
		private NaverResponse response;

		@Getter
		@Setter
		public static class NaverResponse {
			private String id;
			private String email;
			private String name;
			@JsonProperty("profile_image")
			private String profileImage;
		}
	}

	// Kakao OAuth Response
	@Getter
	@Setter
	public static class KakaoOAuthResponse {
		private Long id;
		@JsonProperty("kakao_account")
		private KakaoAccount kakaoAccount;

		@Getter
		@Setter
		public static class KakaoAccount {
			private KakaoProfile profile;
			private String email;

			@Getter
			@Setter
			public static class KakaoProfile {
				private String nickname;
				@JsonProperty("profile_image_url")
				private String profileImageUrl;
			}
		}
	}
}
