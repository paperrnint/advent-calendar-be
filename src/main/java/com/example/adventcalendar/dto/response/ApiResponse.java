package com.example.adventcalendar.dto.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

public record ApiResponse<T>(
	int status,
	String message,
	T data,
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	LocalDateTime timestamp
) {
	public static <T> ApiResponse<T> success(T data) {
		return new ApiResponse<>(200, "success", data, LocalDateTime.now());
	}

	public static <T> ApiResponse<T> success(T data, String message) {
		return new ApiResponse<>(200, message, data, LocalDateTime.now());
	}

	public static <Void> ApiResponse<Void> success() {
		return new ApiResponse<>(200, "success", null, LocalDateTime.now());
	}

	public static <T> ApiResponse<T> error(int status, String message) {
		return new ApiResponse<>(status, message, null, LocalDateTime.now());
	}
}
