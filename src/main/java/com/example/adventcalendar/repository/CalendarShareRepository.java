package com.example.adventcalendar.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.adventcalendar.entity.CalendarShare;

@Repository
public interface CalendarShareRepository extends JpaRepository<CalendarShare, Long> {

	Optional<CalendarShare> findByShareToken(String shareToken);

	Optional<CalendarShare> findByCalendarId(Long calendarId);
}
