// File: src/main/java/com/example/adventcalendar/service/AuthService.java
package com.example.adventcalendar.service;

import com.example.adventcalendar.config.JwtTokenProvider;
import com.example.adventcalendar.constant.UserStatus;
import com.example.adventcalendar.dto.request.UserCreateRequest;
import com.example.adventcalendar.dto.response.LoginResponse;
import com.example.adventcalendar.dto.response.UserCreateResponse;
import com.example.adventcalendar.dto.response.UserRegistrationResult;
import com.example.adventcalendar.entity.RefreshToken;
import com.example.adventcalendar.entity.User;
import com.example.adventcalendar.repository.RefreshTokenRepository;
import com.example.adventcalendar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

	private final OAuth2Service oAuth2Service;
	private final JwtTokenProvider jwtTokenProvider;
	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;


	@Transactional
	public LoginResponse handleNaverCallback(String code, String state) {
		OAuth2Service.OAuthUserInfo userInfo = oAuth2Service.authenticateNaver(code, state);

		Optional<User> existingUser = userRepository.findByOauthProviderAndOauthId(
			userInfo.getOauthProvider(),
			userInfo.getOauthId()
		);

		if (existingUser.isPresent()) {
			User user = existingUser.get();

			if (user.getStatus() == UserStatus.PENDING) {
				return createLoginResponseForPendingUser(user);
			}

			return loginExistingUser(user);
		} else {
			return createTemporaryUser(userInfo);
		}
	}

	@Transactional
	public LoginResponse handleKakaoCallback(String code) {
		OAuth2Service.OAuthUserInfo userInfo = oAuth2Service.authenticateKakao(code);

		Optional<User> existingUser = userRepository.findByOauthProviderAndOauthId(
			userInfo.getOauthProvider(),
			userInfo.getOauthId()
		);

		if (existingUser.isPresent()) {
			User user = existingUser.get();

			if (user.getStatus() == UserStatus.PENDING) {
				return createLoginResponseForPendingUser(user);
			}

			return loginExistingUser(user);
		} else {
			return createTemporaryUser(userInfo);
		}
	}


	private LoginResponse loginExistingUser(User user) {
		String accessToken = jwtTokenProvider.createAccessToken(
			user.getId(),
			user.getEmail(),
			user.getOauthProvider()
		);

		String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

		saveRefreshToken(user.getId(), refreshToken);

		return LoginResponse.forExistingUser(
			accessToken,
			refreshToken,
			jwtTokenProvider.getAccessTokenValidityInSeconds(),
			user.getShareUuid()
		);
	}

	private LoginResponse createLoginResponseForPendingUser(User user) {
		String tempToken = jwtTokenProvider.createTempToken(
			user.getId(),
			user.getEmail(),
			user.getOauthProvider()
		);

		return LoginResponse.forNewUser(
			tempToken,
			5 * 60L
		);
	}

	private LoginResponse createTemporaryUser(OAuth2Service.OAuthUserInfo userInfo) {
		User tempUser = User.builder()
			.email(userInfo.getEmail())
			.name(userInfo.getName())
			.oauthProvider(userInfo.getOauthProvider())
			.oauthId(userInfo.getOauthId())
			.status(UserStatus.PENDING)
			.build();

		tempUser = userRepository.save(tempUser);

		String tempToken = jwtTokenProvider.createTempToken(
			tempUser.getId(),
			tempUser.getEmail(),
			tempUser.getOauthProvider()
		);

		return LoginResponse.forNewUser(
			tempToken,
			5 * 60L  // 5분
		);
	}

	@Transactional
	public UserRegistrationResult completeUserRegistration(Long userId, UserCreateRequest request) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

		if (user.getStatus() == UserStatus.ACTIVE) {
			throw new IllegalStateException("이미 등록이 완료된 사용자입니다");
		}

		if (user.getStatus() != UserStatus.PENDING) {
			throw new IllegalStateException("회원가입을 진행할 수 없는 상태입니다");
		}

		user.completeRegistration(request.getName(), request.getColor());
		user = userRepository.save(user);

		String accessToken = jwtTokenProvider.createAccessToken(
			user.getId(),
			user.getEmail(),
			user.getOauthProvider()
		);

		String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

		refreshTokenRepository.deleteByUserId(user.getId());
		saveRefreshToken(user.getId(), refreshToken);

		return UserRegistrationResult.create(
			user.getShareUuid(),
			accessToken,
			refreshToken
		);
	}

	@Transactional
	public String refreshAccessToken(String refreshToken) {
		if (!jwtTokenProvider.validateToken(refreshToken)) {
			throw new IllegalArgumentException("유효하지 않은 RefreshToken입니다");
		}

		RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
			.orElseThrow(() -> new IllegalArgumentException("RefreshToken을 찾을 수 없습니다"));

		if (storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
			refreshTokenRepository.delete(storedToken);
			throw new IllegalArgumentException("RefreshToken이 만료되었습니다");
		}

		User user = userRepository.findById(storedToken.getUserId())
			.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

		return jwtTokenProvider.createAccessToken(
			user.getId(),
			user.getEmail(),
			user.getOauthProvider()
		);
	}

	@Transactional
	public void logout(String refreshToken) {
		log.debug("로그아웃 처리 시작 - refreshToken: {}", refreshToken);

		Optional<RefreshToken> tokenOptional = refreshTokenRepository.findByToken(refreshToken);

		if (tokenOptional.isPresent()) {
			RefreshToken token = tokenOptional.get();
			log.debug("RefreshToken 찾음 - id: {}, userId: {}", token.getId(), token.getUserId());

			refreshTokenRepository.delete(token);
			refreshTokenRepository.flush();

			log.debug("RefreshToken 삭제 완료");
		} else {
			log.warn("RefreshToken을 찾을 수 없음 - token: {}", refreshToken);
		}
	}

	private void saveRefreshToken(Long userId, String token) {
		RefreshToken refreshToken = RefreshToken.builder()
			.userId(userId)
			.token(token)
			.expiresAt(LocalDateTime.now().plusSeconds(jwtTokenProvider.getRefreshTokenValidityInSeconds()))
			.build();

		refreshTokenRepository.save(refreshToken);
	}

	public void validateUserActive(Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new IllegalStateException("회원가입을 완료해주세요");
		}
	}
}
