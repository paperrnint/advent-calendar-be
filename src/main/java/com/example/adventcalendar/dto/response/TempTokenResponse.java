package com.example.adventcalendar.dto.response;

public record TempTokenResponse(
	String tempToken,
	Long expiresIn,
	String name,
	String email
) {
	public static TempTokenResponse create(
		String tempToken,
		Long expiresInSeconds,
		String name,
		String email
	) {
		return new TempTokenResponse(
			tempToken,
			expiresInSeconds,
			name,
			email
		);
	}
}
