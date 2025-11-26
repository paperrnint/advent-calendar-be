package com.example.adventcalendar.dto.response;

import com.example.adventcalendar.entity.User;

public record UserRegistrationResult(
	User user,
	String accessToken,
	String refreshToken
) {
	public static UserRegistrationResult create(
		User user,
		String accessToken,
		String refreshToken
	) {
		return new UserRegistrationResult(user, accessToken, refreshToken);
	}
}
