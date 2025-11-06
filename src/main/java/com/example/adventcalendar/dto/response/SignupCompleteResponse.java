package com.example.adventcalendar.dto.response;

public record SignupCompleteResponse(
	UserResponse user,
	String calendarUuid,
	String accessToken,
	String refreshToken,
	Long expiresIn
) {
	public static SignupCompleteResponse create(
		UserResponse user,
		String calendarUuid,
		String accessToken,
		String refreshToken,
		Long expiresInSeconds
	) {
		return new SignupCompleteResponse(
			user,
			calendarUuid,
			accessToken,
			refreshToken,
			expiresInSeconds
		);
	}
}
