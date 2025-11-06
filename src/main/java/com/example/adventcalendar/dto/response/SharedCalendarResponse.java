package com.example.adventcalendar.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.example.adventcalendar.entity.Calendar;
import com.example.adventcalendar.entity.Message;
import com.fasterxml.jackson.annotation.JsonFormat;

public record SharedCalendarResponse(
	String selectedColor,
	List<MessageResponse> messages,
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	LocalDateTime createdAt
) {

	public static SharedCalendarResponse fromEntity(
		Calendar calendar,
		List<Message> messages
	) {
		return new SharedCalendarResponse(
			calendar.getSelectedColor(),
			messages.stream()
				.map(MessageResponse::fromEntity)
				.toList(),
			calendar.getCreatedAt()
		);
	}
}
