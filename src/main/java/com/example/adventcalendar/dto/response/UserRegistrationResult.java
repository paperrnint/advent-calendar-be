package com.example.adventcalendar.dto.response;

public record UserRegistrationResult(
	String uuid,
	String accessToken,
	String refreshToken
) {
	public static UserRegistrationResult create(
		String uuid,
		String accessToken,
		String refreshToken
	) {
		return new UserRegistrationResult(uuid, accessToken, refreshToken);
	}
}
