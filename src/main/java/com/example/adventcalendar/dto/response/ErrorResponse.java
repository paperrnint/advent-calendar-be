package com.example.adventcalendar.dto.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

public record ErrorResponse(
	int status,
	String message,
	String errorCode,
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	LocalDateTime timestamp,
	String path
) {
	/**
	 * 에러 응답 생성
	 */
	public static ErrorResponse of(
		int status,
		String message,
		String errorCode,
		String path
	) {
		return new ErrorResponse(
			status,
			message,
			errorCode,
			LocalDateTime.now(),
			path
		);
	}
}
