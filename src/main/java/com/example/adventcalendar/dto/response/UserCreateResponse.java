package com.example.adventcalendar.dto.response;

public record UserCreateResponse(
	String uuid
) {
	public static UserCreateResponse create(String userUuid) {
		return new UserCreateResponse(userUuid);
	}
}
