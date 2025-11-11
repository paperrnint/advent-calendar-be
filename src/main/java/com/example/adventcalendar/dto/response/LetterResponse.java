package com.example.adventcalendar.dto.response;

import java.time.LocalDateTime;

import com.example.adventcalendar.entity.Letter;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LetterResponse {

	private Integer day;
	private String content;
	private String fromName;

	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private LocalDateTime createdAt;

	public static LetterResponse fromEntity(Letter letter) {
		return LetterResponse.builder()
			.day(letter.getDay())
			.content(letter.getContent())
			.fromName(letter.getFromName())
			.createdAt(letter.getCreatedAt())
			.build();
	}
}
