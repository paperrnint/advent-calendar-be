package com.example.adventcalendar.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.adventcalendar.entity.Message;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

	Optional<Message> findByCalendarIdAndDay(Long calendarId, Integer day);

	List<Message> findByCalendarId(Long calendarId);
}
