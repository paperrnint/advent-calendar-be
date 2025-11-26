package com.example.adventcalendar.dto.response;

import com.example.adventcalendar.entity.User;

public record UserCreateResponse(
	String name,
	String color,
	String uuid,
	String email
) {
	public static UserCreateResponse fromEntity(User user) {
		return new UserCreateResponse(
			user.getName(),
			user.getSelectedColor(),
			user.getShareUuid(),
			user.getEmail()
		);
	}
}
