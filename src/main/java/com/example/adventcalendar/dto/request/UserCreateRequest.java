package com.example.adventcalendar.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserCreateRequest {

	@NotBlank(message = "이름은 필수입니다")
	@Size(min = 1, max = 10, message = "이름은 1-10자여야 합니다")
	private String name;

	@NotBlank(message = "색상 선택은 필수입니다")
	@Pattern(
		regexp = "^(brown|red|orange|yellow|pink|lightGreen|green|blue|navy|violet)$",
		message = "유효한 색상을 선택해주세요"
	)
	private String color;
}
