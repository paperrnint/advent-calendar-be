package com.example.adventcalendar.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens", indexes = {
	@Index(name = "idx_user_id", columnList = "user_id"),
	@Index(name = "idx_token", columnList = "token")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long userId;

	@Column(nullable = false, unique = true, length = 500)
	private String token;

	@Column(nullable = false)
	private LocalDateTime expiresAt;
}
