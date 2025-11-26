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
public class UserPublicInfoResponse {

	private String name;
	private String color;
	private String uuid;

	public static UserPublicInfoResponse fromEntity(User user) {
		return UserPublicInfoResponse.builder()
			.name(user.getName())
			.color(user.getSelectedColor())
			.uuid(user.getShareUuid())
			.build();
	}
}
