package com.example.adventcalendar.dto.response;

import java.time.LocalDateTime;

import com.example.adventcalendar.entity.User;
import com.fasterxml.jackson.annotation.JsonFormat;

public record UserResponse(
	Long id,
	String email,
	String name,
	String oauthProvider,
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	LocalDateTime createdAt
) {

	public static UserResponse fromEntity(User user) {
		return new UserResponse(
			user.getId(),
			user.getEmail(),
			user.getName(),
			user.getOauthProvider(),
			user.getCreatedAt()
		);
	}
}
