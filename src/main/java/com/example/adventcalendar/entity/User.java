package com.example.adventcalendar.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users", indexes = {
	@Index(name = "idx_email", columnList = "email"),
	@Index(name = "idx_oauth_id", columnList = "oauth_provider, oauth_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 100)
	private String email;

	@Column(nullable = false, length = 50)
	private String name;

	@Column(length = 500)
	private String profileImageUrl;

	@Column(nullable = false, length = 20)
	private String oauthProvider; // NAVER, KAKAO

	@Column(nullable = false, length = 100)
	private String oauthId;

	@Column(length = 50)
	private String selectedColor;

	@Column(nullable = false)
	@Builder.Default
	private Boolean isActive = true;
}
