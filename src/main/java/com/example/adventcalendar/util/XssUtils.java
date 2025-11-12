package com.example.adventcalendar.util;

import org.owasp.encoder.Encode;
import org.springframework.stereotype.Component;

@Component
public class XssUtils {

	public static String sanitizeHtml(String input) {
		if (input == null) {
			return null;
		}
		return Encode.forHtml(input);
	}

	public static String[] sanitizeHtml(String... inputs) {
		if (inputs == null) {
			return null;
		}

		String[] sanitized = new String[inputs.length];
		for (int i = 0; i < inputs.length; i++) {
			sanitized[i] = sanitizeHtml(inputs[i]);
		}
		return sanitized;
	}

	public static String sanitizeJavaScript(String input) {
		if (input == null) {
			return null;
		}
		return Encode.forJavaScript(input);
	}
}
