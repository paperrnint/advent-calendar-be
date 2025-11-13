package com.example.adventcalendar.controller;

import com.example.adventcalendar.constant.UserStatus;
import com.example.adventcalendar.dto.request.UserCreateRequest;
import com.example.adventcalendar.dto.response.ApiResponse;
import com.example.adventcalendar.dto.response.LoginResponse;
import com.example.adventcalendar.dto.response.UserCreateResponse;
import com.example.adventcalendar.dto.response.UserInfoResponse;
import com.example.adventcalendar.dto.response.UserRegistrationResult;
import com.example.adventcalendar.entity.User;
import com.example.adventcalendar.exception.ResourceNotFoundException;
import com.example.adventcalendar.exception.UnauthorizedException;
import com.example.adventcalendar.service.AuthService;
import com.example.adventcalendar.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

@Slf4j
@Tag(name = "인증", description = "OAuth2 소셜 로그인 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;
	private final UserRepository userRepository;

	@Value("${app.frontend.url}")
	private String frontendUrl;

	@Value("${oauth2.naver.client-id}")
	private String naverClientId;

	@Value("${oauth2.naver.redirect-uri}")
	private String naverRedirectUri;

	@Value("${oauth2.kakao.client-id}")
	private String kakaoClientId;

	@Value("${oauth2.kakao.redirect-uri}")
	private String kakaoRedirectUri;


	@Operation(summary = "네이버 OAuth 시작", description = "네이버 OAuth 인증 페이지로 리다이렉트합니다")
	@GetMapping("/naver")
	public RedirectView startNaverOAuth() {
		String state = java.util.UUID.randomUUID().toString();
		String naverAuthUrl = String.format(
			"https://nid.naver.com/oauth2.0/authorize?response_type=code&client_id=%s&redirect_uri=%s&state=%s",
			naverClientId,
			naverRedirectUri,
			state
		);

		log.info("네이버 OAuth 시작 - 리다이렉트: {}", naverAuthUrl);
		return new RedirectView(naverAuthUrl);
	}

	@Operation(summary = "카카오 OAuth 시작", description = "카카오 OAuth 인증 페이지로 리다이렉트합니다")
	@GetMapping("/kakao")
	public RedirectView startKakaoOAuth() {
		String kakaoAuthUrl = String.format(
			"https://kauth.kakao.com/oauth/authorize?response_type=code&client_id=%s&redirect_uri=%s",
			kakaoClientId,
			kakaoRedirectUri
		);

		log.info("카카오 OAuth 시작 - 리다이렉트: {}", kakaoAuthUrl);
		return new RedirectView(kakaoAuthUrl);
	}


	@Operation(summary = "네이버 OAuth 콜백", description = "네이버 인증 후 콜백을 처리합니다")
	@GetMapping("/oauth/naver/callback")
	public RedirectView naverCallback(
		@Parameter(description = "Authorization code") @RequestParam String code,
		@Parameter(description = "State parameter") @RequestParam String state,
		HttpServletResponse response
	) {
		try {
			log.info("네이버 OAuth 콜백 처리 시작 - code: {}, state: {}", code, state);

			LoginResponse loginResponse = authService.handleNaverCallback(code, state);

			if (loginResponse.isExistingUser()) {
				setRefreshTokenCookie(response, loginResponse.refreshToken());
				setAccessTokenCookie(response, loginResponse.accessToken());

				String redirectUrl = String.format("%s/%s", frontendUrl, loginResponse.userUuid());
				log.info("기존 사용자 로그인 완료 - 리다이렉트: {}", redirectUrl);
				return new RedirectView(redirectUrl);
			} else {
				setTempTokenCookie(response, loginResponse.accessToken());

				String redirectUrl = String.format("%s/new", frontendUrl);
				log.info("신규 사용자 - 리다이렉트: {}", redirectUrl);
				return new RedirectView(redirectUrl);
			}

		} catch (Exception e) {
			log.error("네이버 OAuth 콜백 처리 실패", e);
			return new RedirectView(frontendUrl + "/auth/error?message=" + e.getMessage());
		}
	}

	@Operation(summary = "카카오 OAuth 콜백", description = "카카오 인증 후 콜백을 처리합니다")
	@GetMapping("/oauth/kakao/callback")
	public RedirectView kakaoCallback(
		@Parameter(description = "Authorization code") @RequestParam String code,
		HttpServletResponse response
	) {
		try {
			log.info("카카오 OAuth 콜백 처리 시작 - code: {}", code);

			LoginResponse loginResponse = authService.handleKakaoCallback(code);

			if (loginResponse.isExistingUser()) {
				setRefreshTokenCookie(response, loginResponse.refreshToken());
				setAccessTokenCookie(response, loginResponse.accessToken());

				String redirectUrl = String.format("%s/%s", frontendUrl, loginResponse.userUuid());
				log.info("기존 사용자 로그인 완료 - 리다이렉트: {}", redirectUrl);
				return new RedirectView(redirectUrl);
			} else {
				setTempTokenCookie(response, loginResponse.accessToken());

				String redirectUrl = String.format("%s/new", frontendUrl);
				log.info("신규 사용자 - 리다이렉트: {}", redirectUrl);
				return new RedirectView(redirectUrl);
			}

		} catch (Exception e) {
			log.error("카카오 OAuth 콜백 처리 실패", e);
			return new RedirectView(frontendUrl + "/auth/error?message=" + e.getMessage());
		}
	}

	@Operation(summary = "신규 사용자 등록", description = "신규 사용자의 이름과 색상을 등록합니다")
	@PostMapping("/users")
	public ApiResponse<UserCreateResponse> createUser(
		@Parameter(description = "현재 로그인된 사용자 ID", hidden = true) Authentication authentication,
		@Parameter(description = "사용자 정보", required = true) @Valid @RequestBody UserCreateRequest request,
		HttpServletResponse response
	) {
		if (authentication == null || authentication.getPrincipal() == null) {
			throw new UnauthorizedException("인증이 필요합니다");
		}

		Long userId = (Long) authentication.getPrincipal();
		log.info("신규 사용자 등록 요청 - userId: {}, 이름: {}, 색상: {}", userId, request.getName(), request.getColor());

		UserRegistrationResult result = authService.completeUserRegistration(userId, request);

		deleteTempTokenCookie(response);

		setAccessTokenCookie(response, result.accessToken());
		setRefreshTokenCookie(response, result.refreshToken());

		UserCreateResponse userResponse = UserCreateResponse.create(result.uuid());

		log.info("신규 사용자 등록 완료 - userId: {}, uuid: {}", userId, result.uuid());

		return ApiResponse.success(userResponse, "회원가입이 완료되었습니다");
	}

	@Operation(summary = "현재 사용자 정보 조회", description = "로그인한 사용자의 이름, 색상, UUID를 조회합니다")
	@GetMapping("/me")
	public ApiResponse<UserInfoResponse> getCurrentUser(
		@Parameter(description = "현재 로그인된 사용자 ID", hidden = true) Authentication authentication
	) {
		if (authentication == null || authentication.getPrincipal() == null) {
			throw new UnauthorizedException("인증이 필요합니다");
		}

		Long userId = (Long) authentication.getPrincipal();
		log.info("현재 사용자 정보 조회 요청 - userId: {}", userId);

		User user = userRepository.findById(userId)
			.orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 사용자입니다"));

		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new IllegalStateException("회원가입을 완료해주세요");
		}

		UserInfoResponse response = UserInfoResponse.fromEntity(user);

		return ApiResponse.success(response);
	}

	@Operation(summary = "토큰 갱신", description = "RefreshToken으로 새로운 AccessToken을 발급받습니다")
	@PostMapping("/refresh")
	public ApiResponse<String> refreshToken(
		@CookieValue(name = "refreshToken", required = true) String refreshToken,
		HttpServletResponse response
	) {
		log.info("토큰 갱신 요청");

		String newAccessToken = authService.refreshAccessToken(refreshToken);

		setAccessTokenCookie(response, newAccessToken);

		log.info("토큰 갱신 완료");

		return ApiResponse.success();
	}

	@Operation(summary = "로그아웃", description = "로그아웃하고 RefreshToken을 무효화합니다")
	@PostMapping("/logout")
	public ApiResponse<Void> logout(
		@CookieValue(name = "refreshToken", required = false) String refreshToken,
		HttpServletResponse response
	) {
		log.info("로그아웃 요청");

		if (refreshToken != null) {
			authService.logout(refreshToken);
		}

		Cookie cookie = new Cookie("refreshToken", null);
		cookie.setHttpOnly(true);
		cookie.setSecure(true);
		cookie.setPath("/");
		cookie.setMaxAge(0);
		response.addCookie(cookie);

		log.info("로그아웃 완료");

		return ApiResponse.success();
	}

	private void setTempTokenCookie(HttpServletResponse response, String tempToken) {
		Cookie cookie = new Cookie("tempToken", tempToken);
		cookie.setHttpOnly(true);
		cookie.setSecure(true);
		cookie.setPath("/");
		cookie.setMaxAge(5 * 60);  // 5분

		response.addCookie(cookie);
	}

	private void setAccessTokenCookie(HttpServletResponse response, String accessToken) {
		Cookie cookie = new Cookie("accessToken", accessToken);
		cookie.setHttpOnly(true);
		cookie.setSecure(true);
		cookie.setPath("/");
		cookie.setMaxAge(60 * 60);  // 1시간

		response.addCookie(cookie);
	}

	private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
		Cookie cookie = new Cookie("refreshToken", refreshToken);
		cookie.setHttpOnly(true);
		cookie.setSecure(true);
		cookie.setPath("/");
		cookie.setMaxAge(30 * 24 * 60 * 60);  // 30일

		response.addCookie(cookie);
	}

	private void deleteTempTokenCookie(HttpServletResponse response) {
		Cookie cookie = new Cookie("tempToken", null);
		cookie.setHttpOnly(true);
		cookie.setSecure(true);
		cookie.setPath("/");
		cookie.setMaxAge(0);  // 즉시 삭제

		response.addCookie(cookie);
		log.debug("tempToken 쿠키 삭제 완료");
	}
}
