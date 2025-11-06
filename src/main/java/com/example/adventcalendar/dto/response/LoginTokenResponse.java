package com.example.adventcalendar.dto.response;

public record LoginTokenResponse(
	String accessToken,
	String refreshToken,
	Long expiresIn,
	String tokenType
) {

	public static LoginTokenResponse create(
		String accessToken,
		String refreshToken,
		Long expiresInSeconds
	) {
		return new LoginTokenResponse(
			accessToken,
			refreshToken,
			expiresInSeconds,
			"Bearer"
		);
	}
}
