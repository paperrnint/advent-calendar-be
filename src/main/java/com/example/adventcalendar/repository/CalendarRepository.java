package com.example.adventcalendar.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.adventcalendar.entity.Calendar;

@Repository
public interface CalendarRepository extends JpaRepository<Calendar, Long> {

	Optional<Calendar> findByShareUuid(String shareUuid);

	Optional<Calendar> findByUserIdAndId(Long userId, Long calendarId);

	java.util.List<Calendar> findByUserId(Long userId);
}
