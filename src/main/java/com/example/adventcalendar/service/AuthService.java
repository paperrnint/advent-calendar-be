package com.example.adventcalendar.service;

import com.example.adventcalendar.config.JwtTokenProvider;
import com.example.adventcalendar.dto.request.SignupCompleteRequest;
import com.example.adventcalendar.dto.response.SignupCompleteResponse;
import com.example.adventcalendar.dto.response.TempTokenResponse;
import com.example.adventcalendar.dto.response.UserResponse;
import com.example.adventcalendar.entity.Calendar;
import com.example.adventcalendar.entity.User;
import com.example.adventcalendar.repository.CalendarRepository;
import com.example.adventcalendar.repository.UserRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

	private final OAuth2Service oAuth2Service;
	private final JwtTokenProvider jwtTokenProvider;
	private final UserRepository userRepository;
	private final CalendarRepository calendarRepository;

	@Transactional
	public TempTokenResponse handleNaverCallback(String code, String state) {
		OAuth2Service.OAuthUserInfo userInfo = oAuth2Service.authenticateNaver(code, state);

		Optional<User> existingUser = userRepository.findByOauthProviderAndOauthId(
			userInfo.getOauthProvider(),
			userInfo.getOauthId()
		);

		String tempToken = jwtTokenProvider.createTempToken(
			userInfo.getOauthProvider(),
			userInfo.getOauthId(),
			userInfo.getEmail(),
			userInfo.getName(),
			userInfo.getProfileImageUrl()
		);

		return TempTokenResponse.create(
			tempToken,
			300L, // 5분
			userInfo.getName(),
			userInfo.getEmail()
		);
	}

	@Transactional
	public TempTokenResponse handleKakaoCallback(String code) {
		OAuth2Service.OAuthUserInfo userInfo = oAuth2Service.authenticateKakao(code);

		Optional<User> existingUser = userRepository.findByOauthProviderAndOauthId(
			userInfo.getOauthProvider(),
			userInfo.getOauthId()
		);

		String tempToken = jwtTokenProvider.createTempToken(
			userInfo.getOauthProvider(),
			userInfo.getOauthId(),
			userInfo.getEmail(),
			userInfo.getName(),
			userInfo.getProfileImageUrl()
		);

		return TempTokenResponse.create(
			tempToken,
			300L, // 5분
			userInfo.getName(),
			userInfo.getEmail()
		);
	}

	@Transactional
	public SignupCompleteResponse completeSignup(String tempToken, SignupCompleteRequest request) {
		if (!jwtTokenProvider.validateToken(tempToken)) {
			throw new RuntimeException("유효하지 않은 토큰입니다");
		}

		Claims claims = jwtTokenProvider.parseClaims(tempToken);
		String tokenType = claims.get("type", String.class);

		if (!"temp".equals(tokenType)) {
			throw new RuntimeException("임시 토큰이 아닙니다");
		}

		String oauthProvider = claims.get("oauthProvider", String.class);
		String oauthId = claims.get("oauthId", String.class);
		String email = claims.get("email", String.class);
		String profileImage = claims.get("profileImage", String.class);

		Optional<User> existingUser = userRepository.findByOauthProviderAndOauthId(
			oauthProvider,
			oauthId
		);

		User user;
		Calendar calendar;

		if (existingUser.isPresent()) {
			user = existingUser.get();
			user.setName(request.getName());
			user.setSelectedColor(request.getSelectedColor());
			user = userRepository.save(user);

			calendar = calendarRepository.findByUserId(user.getId())
				.stream()
				.findFirst()
				.orElseThrow(() -> new RuntimeException("캘린더를 찾을 수 없습니다"));

			calendar.setSelectedColor(request.getSelectedColor());
			calendar = calendarRepository.save(calendar);
		} else {
			user = User.builder()
				.email(email)
				.name(request.getName())
				.oauthProvider(oauthProvider)
				.oauthId(oauthId)
				.selectedColor(request.getSelectedColor())
				.isActive(true)
				.build();
			user = userRepository.save(user);

			calendar = Calendar.builder()
				.user(user)
				.selectedColor(request.getSelectedColor())
				.build();
			calendar = calendarRepository.save(calendar);
		}

		String accessToken = jwtTokenProvider.createAccessToken(
			user.getId(),
			user.getEmail(),
			user.getOauthProvider()
		);

		String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

		return SignupCompleteResponse.create(
			UserResponse.fromEntity(user),
			calendar.getShareUuid(),
			accessToken,
			refreshToken,
			jwtTokenProvider.getAccessTokenValidityInSeconds()
		);
	}

	@Transactional(readOnly = true)
	public SignupCompleteResponse loginExistingUser(String tempToken) {
		if (!jwtTokenProvider.validateToken(tempToken)) {
			throw new RuntimeException("유효하지 않은 토큰입니다");
		}

		Claims claims = jwtTokenProvider.parseClaims(tempToken);
		String oauthProvider = claims.get("oauthProvider", String.class);
		String oauthId = claims.get("oauthId", String.class);

		User user = userRepository.findByOauthProviderAndOauthId(oauthProvider, oauthId)
			.orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

		Calendar calendar = calendarRepository.findByUserId(user.getId())
			.stream()
			.findFirst()
			.orElseThrow(() -> new RuntimeException("캘린더를 찾을 수 없습니다"));

		String accessToken = jwtTokenProvider.createAccessToken(
			user.getId(),
			user.getEmail(),
			user.getOauthProvider()
		);

		String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

		return SignupCompleteResponse.create(
			UserResponse.fromEntity(user),
			calendar.getShareUuid(),
			accessToken,
			refreshToken,
			jwtTokenProvider.getAccessTokenValidityInSeconds()
		);
	}
}
