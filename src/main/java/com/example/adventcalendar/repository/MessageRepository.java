package com.example.adventcalendar.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.adventcalendar.entity.Message;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

	Optional<Message> findByUserIdAndDay(Long userId, Integer day);

	List<Message> findByUserId(Long userId);

	List<Message> findByUserIdAndDayLessThanEqual(Long userId, Integer day);
}
