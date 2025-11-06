package com.example.adventcalendar.config;

import com.example.adventcalendar.config.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtTokenProvider jwtTokenProvider;

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {

		try {
			String token = resolveToken(request);

			if (token != null && jwtTokenProvider.validateToken(token)) {
				Claims claims = jwtTokenProvider.parseClaims(token);
				String tokenType = claims.get("type", String.class);

				if ("access".equals(tokenType)) {
					Long userId = Long.parseLong(claims.getSubject());
					String email = claims.get("email", String.class);

					Authentication authentication = new UsernamePasswordAuthenticationToken(
						userId,
						null,
						List.of(new SimpleGrantedAuthority("ROLE_USER"))
					);

					SecurityContextHolder.getContext().setAuthentication(authentication);

					log.debug("JWT 인증 성공 - userId: {}, email: {}", userId, email);
				}
			}

		} catch (Exception e) {
			log.error("JWT 인증 실패", e);
		}

		filterChain.doFilter(request, response);
	}

	private String resolveToken(HttpServletRequest request) {
		String bearerToken = request.getHeader("Authorization");

		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
			return bearerToken.substring(7);
		}

		return null;
	}
}
