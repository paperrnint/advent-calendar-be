package com.example.adventcalendar.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LetterCreateRequest {

	@NotNull(message = "날짜는 필수입니다")
	@Min(value = 1, message = "날짜는 1일부터 25일까지입니다")
	@Max(value = 25, message = "날짜는 1일부터 25일까지입니다")
	private Integer day;

	@NotBlank(message = "편지 내용은 필수입니다")
	@Size(max = 1000, message = "편지 내용은 1000자 이내여야 합니다")
	private String content;

	@NotBlank(message = "보내는 사람 이름은 필수입니다")
	@Size(min = 1, max = 50, message = "보내는 사람 이름은 1-50자여야 합니다")
	private String fromName;
}
