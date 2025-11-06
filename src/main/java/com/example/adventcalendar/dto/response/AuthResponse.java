package com.example.adventcalendar.dto.response;

import com.example.adventcalendar.entity.User;

public record AuthResponse(
	UserResponse user,
	String accessToken,
	String refreshToken,
	Long expiresIn,
	Boolean needsSetup  // ✅ 추가! 캘린더 설정 필요 여부
) {
	/**
	 * 사용자 생성 후 인증 응답 생성
	 */
	public static AuthResponse fromUser(
		User user,
		String accessToken,
		String refreshToken,
		Long expiresInSeconds,
		boolean needsSetup
	) {
		return new AuthResponse(
			UserResponse.fromEntity(user),
			accessToken,
			refreshToken,
			expiresInSeconds,
			needsSetup
		);
	}
}
