package com.example.adventcalendar.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "messages", indexes = {
	@Index(name = "idx_user_id", columnList = "user_id"),
	@Index(name = "idx_user_day", columnList = "user_id, day")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false)
	private Integer day;

	@Column(nullable = false, length = 100)
	private String toName;

	@Column(nullable = false, length = 100)
	private String fromName;

	@Column(nullable = false, columnDefinition = "LONGTEXT")
	private String messageContent;
}
