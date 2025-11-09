package com.example.adventcalendar.dto.response;

public record UserCreateResponse(
	Long userId,
	String userUuid,
	String accessToken,
	String refreshToken,
	Long expiresIn
) {

	public static UserCreateResponse create(
		Long userId,
		String userUuid,
		String accessToken,
		String refreshToken,
		Long expiresInSeconds
	) {
		return new UserCreateResponse(
			userId,
			userUuid,
			accessToken,
			refreshToken,
			expiresInSeconds
		);
	}
}
