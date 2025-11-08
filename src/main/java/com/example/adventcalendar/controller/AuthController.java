package com.example.adventcalendar.controller;

import com.example.adventcalendar.dto.request.SignupCompleteRequest;
import com.example.adventcalendar.dto.response.ApiResponse;
import com.example.adventcalendar.dto.response.SignupCompleteResponse;
import com.example.adventcalendar.dto.response.TempTokenResponse;
import com.example.adventcalendar.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Tag(name = "인증", description = "OAuth2 소셜 로그인 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@Value("${app.frontend.url}")
	private String frontendUrl;


	@Operation(summary = "네이버 OAuth 콜백", description = "네이버 인증 후 리다이렉트되어 임시 토큰을 발급합니다")
	@GetMapping("/oauth/naver/callback")
	public RedirectView naverCallback(
		@Parameter(description = "Authorization code") @RequestParam String code,
		@Parameter(description = "State parameter") @RequestParam String state
	) {
		try {
			log.info("네이버 OAuth 콜백 처리 시작 - code: {}, state: {}", code, state);

			TempTokenResponse response = authService.handleNaverCallback(code, state);

			//URL 인코딩 추가
			String encodedTempToken = URLEncoder.encode(response.tempToken(), StandardCharsets.UTF_8);
			String encodedName = URLEncoder.encode(response.name(), StandardCharsets.UTF_8);
			String encodedEmail = URLEncoder.encode(response.email(), StandardCharsets.UTF_8);

			String redirectUrl = String.format(
				"%s/auth/signup?tempToken=%s&name=%s&email=%s",
				frontendUrl,
				encodedTempToken,
				encodedName,
				encodedEmail
			);

			log.info("네이버 OAuth 콜백 처리 완료 - 리다이렉트: {}", redirectUrl);
			return new RedirectView(redirectUrl);

		} catch (Exception e) {
			log.error("네이버 OAuth 콜백 처리 실패", e);

			//에러 메시지도 URL 인코딩
			String encodedMessage = URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
			return new RedirectView(frontendUrl + "/auth/error?message=" + encodedMessage);
		}
	}

	@Operation(summary = "카카오 OAuth 콜백", description = "카카오 인증 후 리다이렉트되어 임시 토큰을 발급합니다")
	@GetMapping("/oauth/kakao/callback")
	public RedirectView kakaoCallback(
		@Parameter(description = "Authorization code") @RequestParam String code
	) {
		try {
			log.info("카카오 OAuth 콜백 처리 시작 - code: {}", code);

			TempTokenResponse response = authService.handleKakaoCallback(code);

			//URL 인코딩 추가
			String encodedTempToken = URLEncoder.encode(response.tempToken(), StandardCharsets.UTF_8);
			String encodedName = URLEncoder.encode(response.name(), StandardCharsets.UTF_8);
			String encodedEmail = URLEncoder.encode(response.email(), StandardCharsets.UTF_8);

			String redirectUrl = String.format(
				"%s/auth/signup?tempToken=%s&name=%s&email=%s",
				frontendUrl,
				encodedTempToken,
				encodedName,
				encodedEmail
			);

			log.info("카카오 OAuth 콜백 처리 완료 - 리다이렉트: {}", redirectUrl);
			return new RedirectView(redirectUrl);

		} catch (Exception e) {
			log.error("카카오 OAuth 콜백 처리 실패", e);

			//에러 메시지도 URL 인코딩
			String encodedMessage = URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
			return new RedirectView(frontendUrl + "/auth/error?message=" + encodedMessage);
		}
	}

	@Operation(summary = "회원가입 완료", description = "임시 토큰과 함께 이름, 색상을 받아 회원가입을 완료합니다")
	@PostMapping("/signup/complete")
	public ApiResponse<SignupCompleteResponse> completeSignup(
		@Parameter(description = "임시 토큰", required = true)
		@RequestHeader("X-Temp-Token") String tempToken,

		@Parameter(description = "회원가입 정보", required = true)
		@Valid @RequestBody SignupCompleteRequest request
	) {
		log.info("회원가입 완료 요청 - 이름: {}, 색상: {}", request.getName(), request.getSelectedColor());

		SignupCompleteResponse response = authService.completeSignup(tempToken, request);

		log.info("회원가입 완료 - userId: {}, calendarUuid: {}",
			response.user().id(),
			response.calendarUuid()
		);

		return ApiResponse.success(response, "회원가입이 완료되었습니다");
	}

	@Operation(summary = "기존 사용자 로그인", description = "기존 사용자가 임시 토큰으로 로그인합니다")
	@PostMapping("/login")
	public ApiResponse<SignupCompleteResponse> login(
		@Parameter(description = "임시 토큰", required = true)
		@RequestHeader("X-Temp-Token") String tempToken
	) {
		log.info("기존 사용자 로그인 요청");

		SignupCompleteResponse response = authService.loginExistingUser(tempToken);

		log.info("기존 사용자 로그인 완료 - userId: {}, calendarUuid: {}",
			response.user().id(),
			response.calendarUuid()
		);

		return ApiResponse.success(response, "로그인되었습니다");
	}

	@Operation(summary = "네이버 로그인 URL", description = "네이버 OAuth 인증을 위한 URL을 반환합니다")
	@GetMapping("/naver/url")
	public ApiResponse<String> getNaverLoginUrl(
		@Value("${oauth2.naver.client-id}") String clientId,
		@Value("${oauth2.naver.redirect-uri}") String redirectUri
	) {
		String state = java.util.UUID.randomUUID().toString();
		String naverAuthUrl = String.format(
			"https://nid.naver.com/oauth2.0/authorize?response_type=code&client_id=%s&redirect_uri=%s&state=%s",
			clientId,
			redirectUri,
			state
		);

		return ApiResponse.success(naverAuthUrl);
	}

	@Operation(summary = "카카오 로그인 URL", description = "카카오 OAuth 인증을 위한 URL을 반환합니다")
	@GetMapping("/kakao/url")
	public ApiResponse<String> getKakaoLoginUrl(
		@Value("${oauth2.kakao.client-id}") String clientId,
		@Value("${oauth2.kakao.redirect-uri}") String redirectUri
	) {
		String kakaoAuthUrl = String.format(
			"https://kauth.kakao.com/oauth/authorize?response_type=code&client_id=%s&redirect_uri=%s",
			clientId,
			redirectUri
		);

		return ApiResponse.success(kakaoAuthUrl);
	}
}
