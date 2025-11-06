package com.example.adventcalendar.dto.response;

import java.time.LocalDateTime;

import com.example.adventcalendar.entity.Message;
import com.fasterxml.jackson.annotation.JsonFormat;

public record MessageResponse(
	Long id,
	Integer day,
	String toName,
	String fromName,
	String messageContent,
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	LocalDateTime createdAt
) {
	/**
	 * Message Entity에서 MessageResponse로 변환
	 */
	public static MessageResponse fromEntity(Message message) {
		return new MessageResponse(
			message.getId(),
			message.getDay(),
			message.getToName(),
			message.getFromName(),
			message.getMessageContent(),
			message.getCreatedAt()
		);
	}
}
