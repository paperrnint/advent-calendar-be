package com.example.adventcalendar.entity;

import java.util.UUID;

import com.example.adventcalendar.constant.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
	@Index(name = "idx_oauth_id", columnList = "oauth_provider, oauth_id"),
	@Index(name = "idx_share_uuid", columnList = "share_uuid")
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

	@Column(nullable = false, length = 100)
	private String email;

	@Column(nullable = false, length = 50)
	private String name;

	@Column(nullable = false, length = 20)
	private String oauthProvider; // NAVER, KAKAO

	@Column(nullable = false, length = 100)
	private String oauthId;

	@Column(length = 50)
	private String selectedColor;

	@Column(unique = true, length = 36)
	private String shareUuid; // 캘린더 공유용 UUID

	@Column(nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	@Builder.Default
	private UserStatus status = UserStatus.PENDING;

	@Column(nullable = false)
	@Builder.Default
	private Boolean isActive = true;


	public void completeRegistration(String name, String selectedColor) {
		this.name = name;
		this.selectedColor = selectedColor;
		this.shareUuid = UUID.randomUUID().toString();
		this.status = UserStatus.ACTIVE;
	}
}
