package com.example.adventcalendar.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.adventcalendar.entity.Letter;

@Repository
public interface LetterRepository extends JpaRepository<Letter, Long> {

	List<Letter> findByUserId(Long userId);

	List<Letter> findByUserIdAndDayLessThanEqual(Long userId, Integer day);

	List<Letter> findByUserIdAndDay(Long userId, Integer day);

	@Query("SELECT l.day, COUNT(l) FROM Letter l WHERE l.user.id = :userId GROUP BY l.day")
	List<Object[]> countByUserIdGroupByDay(@Param("userId") Long userId);
}
