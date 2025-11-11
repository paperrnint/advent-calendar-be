package com.example.adventcalendar.dto.response;

public record LoginResponse(
	String accessToken,
	String refreshToken,
	Long expiresIn,
	boolean isExistingUser,
	String userUuid  // 기존 사용자인 경우에만 존재
) {

	public static LoginResponse forExistingUser(
		String accessToken,
		String refreshToken,
		Long expiresInSeconds,
		String userUuid
	) {
		return new LoginResponse(
			accessToken,
			refreshToken,
			expiresInSeconds,
			true,
			userUuid
		);
	}

	public static LoginResponse forNewUser(
		String tempToken,
		Long expiresInSeconds
	) {
		return new LoginResponse(
			tempToken,
			null,
			expiresInSeconds,
			false,
			null
		);
	}
}
