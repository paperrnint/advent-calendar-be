package com.example.adventcalendar.config;

import com.example.adventcalendar.dto.response.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			.csrf(AbstractHttpConfigurer::disable)

			.cors(cors -> cors.configurationSource(corsConfigurationSource()))

			.sessionManagement(session ->
				session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
			)

			.exceptionHandling(exception -> exception
				.authenticationEntryPoint(customAuthenticationEntryPoint())
				.accessDeniedHandler(customAccessDeniedHandler())
			)

			.authorizeHttpRequests(auth -> auth
				.requestMatchers(
					"/api/auth/naver",
					"/api/auth/kakao",
					"/api/auth/oauth/**"
				).permitAll()

				.requestMatchers(
					"/api/auth/refresh",
					"/api/auth/logout"
				).permitAll()

				.requestMatchers(HttpMethod.GET, "/api/users/{uuid}").permitAll()

				.requestMatchers(HttpMethod.POST, "/api/*/letters").permitAll()

				.requestMatchers(
					"/swagger-ui/**",
					"/v3/api-docs/**",
					"/swagger-resources/**",
					"/actuator/**"
				).permitAll()

				.anyRequest().authenticated()
			)

			.addFilterBefore(
				jwtAuthenticationFilter,
				UsernamePasswordAuthenticationFilter.class
			);

		return http.build();
	}

	@Bean
	public AuthenticationEntryPoint customAuthenticationEntryPoint() {
		return (request, response, authException) -> {
			log.warn("인증 실패 - URI: {}, Message: {}",
				request.getRequestURI(),
				authException.getMessage());

			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.setContentType("application/json");
			response.setCharacterEncoding("UTF-8");

			ErrorResponse errorResponse = ErrorResponse.of(
				HttpStatus.UNAUTHORIZED.value(),
				"인증이 필요합니다",
				"UNAUTHORIZED",
				request.getRequestURI()
			);

			ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.registerModule(new JavaTimeModule());
			response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
		};
	}

	@Bean
	public AccessDeniedHandler customAccessDeniedHandler() {
		return (request, response, accessDeniedException) -> {
			log.warn("권한 없음 - URI: {}, Message: {}",
				request.getRequestURI(),
				accessDeniedException.getMessage());

			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			response.setContentType("application/json");
			response.setCharacterEncoding("UTF-8");

			ErrorResponse errorResponse = ErrorResponse.of(
				HttpStatus.FORBIDDEN.value(),
				"접근 권한이 없습니다",
				"FORBIDDEN",
				request.getRequestURI()
			);

			ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.registerModule(new JavaTimeModule());
			response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
		};
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();

		configuration.setAllowedOrigins(Arrays.asList(
			"http://localhost:3000",
			"http://localhost:3001"
		));

		configuration.setAllowedMethods(Arrays.asList(
			"GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
		));

		configuration.setAllowedHeaders(Arrays.asList(
			"Authorization",
			"Content-Type",
			"X-Requested-With"
		));

		configuration.setAllowCredentials(true);

		configuration.setMaxAge(3600L);

		configuration.setExposedHeaders(List.of("Authorization"));

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);

		return source;
	}
}
