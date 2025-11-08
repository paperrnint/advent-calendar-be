package com.example.adventcalendar.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.example.adventcalendar.entity.Message;
import com.fasterxml.jackson.annotation.JsonFormat;

public record CalendarResponse(
	Long id,
	String selectedColor,
	String shareUuid,  // ✅ 추가!
	String shareUrl,   // ✅ 추가! (프론트에서 직접 사용 가능)
	List<MessageResponse> messages,
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	LocalDateTime createdAt
) {

	public static CalendarResponse fromEntity(
		Calendar calendar,
		List<Message> messages,
		String baseUrl
	) {
		String shareUrl = baseUrl + "/calendar/" + calendar.getShareUuid();

		return new CalendarResponse(
			calendar.getId(),
			calendar.getSelectedColor(),
			calendar.getShareUuid(),
			shareUrl,
			messages.stream()
				.map(MessageResponse::fromEntity)
				.toList(),
			calendar.getCreatedAt()
		);
	}
}
