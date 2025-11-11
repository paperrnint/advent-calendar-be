package com.example.adventcalendar.dto.response;

import com.example.adventcalendar.entity.User;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponse {

	private String name;
	private String color;
	private String uuid;

	public static UserInfoResponse fromEntity(User user) {
		return UserInfoResponse.builder()
			.name(user.getName())
			.color(user.getSelectedColor())
			.uuid(user.getShareUuid())
			.build();
	}
}
