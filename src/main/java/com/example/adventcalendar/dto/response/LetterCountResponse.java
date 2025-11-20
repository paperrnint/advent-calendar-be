package com.example.adventcalendar.dto.response;

import java.util.HashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LetterCountResponse {

	private Map<Integer, Long> counts;

	public static LetterCountResponse create(Map<Integer, Long> counts) {
		Map<Integer, Long> allDays = new HashMap<>();
		for (int day = 1; day <= 25; day++) {
			allDays.put(day, counts.getOrDefault(day, 0L));
		}

		return LetterCountResponse.builder()
			.counts(allDays)
			.build();
	}
}
