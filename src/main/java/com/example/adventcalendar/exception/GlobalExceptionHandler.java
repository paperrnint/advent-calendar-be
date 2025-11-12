package com.example.adventcalendar.exception;

import com.example.adventcalendar.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidationException(
		MethodArgumentNotValidException ex,
		HttpServletRequest request
	) {
		Map<String, String> errors = new HashMap<>();
		ex.getBindingResult().getAllErrors().forEach((error) -> {
			String fieldName = ((FieldError) error).getField();
			String errorMessage = error.getDefaultMessage();
			errors.put(fieldName, errorMessage);
		});

		String message = "입력값 검증에 실패했습니다: " + errors;

		ErrorResponse errorResponse = ErrorResponse.of(
			HttpStatus.BAD_REQUEST.value(),
			message,
			"VALIDATION_FAILED",
			request.getRequestURI()
		);

		log.warn("Validation 예외 발생: {}", errors);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
		ResourceNotFoundException ex,
		HttpServletRequest request
	) {
		ErrorResponse errorResponse = ErrorResponse.of(
			HttpStatus.NOT_FOUND.value(),
			ex.getMessage(),
			"RESOURCE_NOT_FOUND",
			request.getRequestURI()
		);

		log.warn("리소스를 찾을 수 없음: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
	}

	@ExceptionHandler(UnauthorizedException.class)
	public ResponseEntity<ErrorResponse> handleUnauthorizedException(
		UnauthorizedException ex,
		HttpServletRequest request
	) {
		ErrorResponse errorResponse = ErrorResponse.of(
			HttpStatus.UNAUTHORIZED.value(),
			ex.getMessage(),
			"UNAUTHORIZED",
			request.getRequestURI()
		);

		log.warn("인증 실패: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
	}

	@ExceptionHandler(ForbiddenException.class)
	public ResponseEntity<ErrorResponse> handleForbiddenException(
		ForbiddenException ex,
		HttpServletRequest request
	) {
		ErrorResponse errorResponse = ErrorResponse.of(
			HttpStatus.FORBIDDEN.value(),
			ex.getMessage(),
			"FORBIDDEN",
			request.getRequestURI()
		);

		log.warn("권한 없음: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
	}

	@ExceptionHandler(ConflictException.class)
	public ResponseEntity<ErrorResponse> handleConflictException(
		ConflictException ex,
		HttpServletRequest request
	) {
		ErrorResponse errorResponse = ErrorResponse.of(
			HttpStatus.CONFLICT.value(),
			ex.getMessage(),
			"CONFLICT",
			request.getRequestURI()
		);

		log.warn("충돌 발생: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
		IllegalArgumentException ex,
		HttpServletRequest request
	) {
		ErrorResponse errorResponse = ErrorResponse.of(
			HttpStatus.BAD_REQUEST.value(),
			ex.getMessage(),
			"INVALID_ARGUMENT",
			request.getRequestURI()
		);

		log.warn("잘못된 인자 예외 발생: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
	}

	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<ErrorResponse> handleIllegalStateException(
		IllegalStateException ex,
		HttpServletRequest request
	) {
		ErrorResponse errorResponse = ErrorResponse.of(
			HttpStatus.BAD_REQUEST.value(),
			ex.getMessage(),
			"ILLEGAL_STATE",
			request.getRequestURI()
		);

		log.warn("잘못된 상태 예외 발생: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
	}

	@ExceptionHandler(RuntimeException.class)
	public ResponseEntity<ErrorResponse> handleRuntimeException(
		RuntimeException ex,
		HttpServletRequest request
	) {
		ErrorResponse errorResponse = ErrorResponse.of(
			HttpStatus.INTERNAL_SERVER_ERROR.value(),
			ex.getMessage(),
			"INTERNAL_ERROR",
			request.getRequestURI()
		);

		log.error("런타임 예외 발생", ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleException(
		Exception ex,
		HttpServletRequest request
	) {
		ErrorResponse errorResponse = ErrorResponse.of(
			HttpStatus.INTERNAL_SERVER_ERROR.value(),
			"서버 내부 오류가 발생했습니다",
			"INTERNAL_SERVER_ERROR",
			request.getRequestURI()
		);

		log.error("예상치 못한 예외 발생", ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
	}
}
