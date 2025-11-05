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
	@Index(name = "idx_calendar_id", columnList = "calendar_id"),
	@Index(name = "idx_calendar_day", columnList = "calendar_id, day")
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
	@JoinColumn(name = "calendar_id", nullable = false)
	private Calendar calendar;

	@Column(nullable = false)
	private Integer day; // 1-25

	@Column(nullable = false, length = 100)
	private String toName;

	@Column(nullable = false, length = 100)
	private String fromName; // 익명의 이름

	@Column(nullable = false, columnDefinition = "LONGTEXT")
	private String messageContent;
}
