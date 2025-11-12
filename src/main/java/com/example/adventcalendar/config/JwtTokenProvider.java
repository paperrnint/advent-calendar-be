package com.example.adventcalendar.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

	private final SecretKey secretKey;
	private final long accessTokenValidityInMilliseconds;
	private final long refreshTokenValidityInMilliseconds;

	public JwtTokenProvider(
		@Value("${jwt.secret}") String secret,
		@Value("${jwt.access-token-validity-in-seconds}") long accessTokenValidityInSeconds,
		@Value("${jwt.refresh-token-validity-in-seconds}") long refreshTokenValidityInSeconds
	) {
		this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.accessTokenValidityInMilliseconds = accessTokenValidityInSeconds * 1000;
		this.refreshTokenValidityInMilliseconds = refreshTokenValidityInSeconds * 1000;
	}

	public String createAccessToken(Long userId, String email, String oauthProvider) {
		Date now = new Date();
		Date validity = new Date(now.getTime() + accessTokenValidityInMilliseconds);

		return Jwts.builder()
			.setSubject(userId.toString())
			.claim("email", email)
			.claim("oauthProvider", oauthProvider)
			.claim("type", "access")
			.setIssuedAt(now)
			.setExpiration(validity)
			.signWith(secretKey, SignatureAlgorithm.HS512)
			.compact();
	}

	public String createRefreshToken(Long userId) {
		Date now = new Date();
		Date validity = new Date(now.getTime() + refreshTokenValidityInMilliseconds);

		return Jwts.builder()
			.setSubject(userId.toString())
			.claim("type", "refresh")
			.setIssuedAt(now)
			.setExpiration(validity)
			.signWith(secretKey, SignatureAlgorithm.HS512)
			.compact();
	}

	public String createTempToken(Long userId, String email, String oauthProvider) {
		Date now = new Date();
		Date validity = new Date(now.getTime() + 5 * 60 * 1000);  // 5분

		return Jwts.builder()
			.setSubject(userId.toString())
			.claim("email", email)
			.claim("oauthProvider", oauthProvider)
			.claim("type", "temp")
			.setIssuedAt(now)
			.setExpiration(validity)
			.signWith(secretKey, SignatureAlgorithm.HS512)
			.compact();
	}

	public Long getUserId(String token) {
		Claims claims = parseClaims(token);
		return Long.parseLong(claims.getSubject());
	}

	public boolean validateToken(String token) {
		try {
			Jwts.parser()
				.setSigningKey(secretKey)
				.build()
				.parseClaimsJws(token);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public Claims parseClaims(String token) {
		return Jwts.parser()
			.setSigningKey(secretKey)
			.build()
			.parseClaimsJws(token)
			.getBody();
	}

	public long getAccessTokenValidityInSeconds() {
		return accessTokenValidityInMilliseconds / 1000;
	}

	public long getRefreshTokenValidityInSeconds() {
		return refreshTokenValidityInMilliseconds / 1000;
	}
}
