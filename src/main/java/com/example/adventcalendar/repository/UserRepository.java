package com.example.adventcalendar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import com.example.adventcalendar.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByEmail(String email);

	Optional<User> findByOauthProviderAndOauthId(String oauthProvider, String oauthId);

	Optional<User> findByShareUuid(String shareUuid);
}
