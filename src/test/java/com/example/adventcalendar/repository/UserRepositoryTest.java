package com.example.adventcalendar.repository;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.adventcalendar.constant.UserStatus;
import com.example.adventcalendar.entity.User;

import java.util.Optional;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("UserRepository 통합 테스트")
class UserRepositoryTest {

	@Autowired
	private UserRepository userRepository;

	private User activeUser;
	private User pendingUser;

	@BeforeEach
	void setUp() {
		userRepository.deleteAll();

		activeUser = User.builder()
			.email("test@example.com")
			.name("테스트")
			.oauthProvider("NAVER")
			.oauthId("naver123")
			.selectedColor("green")
			.shareUuid("test-uuid-123")
			.status(UserStatus.ACTIVE)
			.build();

		pendingUser = User.builder()
			.email("pending@example.com")
			.name("대기중")
			.oauthProvider("KAKAO")
			.oauthId("kakao456")
			.status(UserStatus.PENDING)
			.build();
	}

	@Nested
	@DisplayName("findByEmail 테스트")
	class FindByEmail {

		@Test
		@DisplayName("이메일로 사용자 조회 성공")
		void findByEmail_Success() {
			// given
			userRepository.save(activeUser);

			// when
			Optional<User> found = userRepository.findByEmail("test@example.com");

			// then
			assertThat(found).isPresent();
			assertThat(found.get().getEmail()).isEqualTo("test@example.com");
			assertThat(found.get().getName()).isEqualTo("테스트");
			assertThat(found.get().getOauthProvider()).isEqualTo("NAVER");
		}

		@Test
		@DisplayName("존재하지 않는 이메일 조회 시 빈 Optional 반환")
		void findByEmail_NotFound_ReturnsEmpty() {
			// when
			Optional<User> found = userRepository.findByEmail("nonexistent@example.com");

			// then
			assertThat(found).isEmpty();
		}

		@Test
		@DisplayName("대소문자 구분하여 조회")
		void findByEmail_CaseSensitive() {
			// given
			userRepository.save(activeUser);

			// when
			Optional<User> found = userRepository.findByEmail("TEST@EXAMPLE.COM");

			// then
			assertThat(found).isEmpty(); // 대소문자가 다르면 조회 안됨
		}

		@Test
		@DisplayName("여러 사용자 중 특정 이메일 조회")
		void findByEmail_MultipleUsers_FindsCorrectOne() {
			// given
			userRepository.save(activeUser);
			userRepository.save(pendingUser);

			// when
			Optional<User> found = userRepository.findByEmail("pending@example.com");

			// then
			assertThat(found).isPresent();
			assertThat(found.get().getEmail()).isEqualTo("pending@example.com");
			assertThat(found.get().getOauthProvider()).isEqualTo("KAKAO");
		}
	}

	@Nested
	@DisplayName("findByOauthProviderAndOauthId 테스트")
	class FindByOauthProviderAndOauthId {

		@Test
		@DisplayName("OAuth Provider와 OAuth ID로 사용자 조회 성공")
		void findByOauthProviderAndOauthId_Success() {
			// given
			userRepository.save(activeUser);

			// when
			Optional<User> found = userRepository.findByOauthProviderAndOauthId("NAVER", "naver123");

			// then
			assertThat(found).isPresent();
			assertThat(found.get().getOauthProvider()).isEqualTo("NAVER");
			assertThat(found.get().getOauthId()).isEqualTo("naver123");
			assertThat(found.get().getEmail()).isEqualTo("test@example.com");
		}

		@Test
		@DisplayName("존재하지 않는 OAuth 정보로 조회 시 빈 Optional 반환")
		void findByOauthProviderAndOauthId_NotFound_ReturnsEmpty() {
			// when
			Optional<User> found = userRepository.findByOauthProviderAndOauthId("GOOGLE", "google999");

			// then
			assertThat(found).isEmpty();
		}

		@Test
		@DisplayName("Provider는 맞지만 ID가 다른 경우")
		void findByOauthProviderAndOauthId_WrongId_ReturnsEmpty() {
			// given
			userRepository.save(activeUser);

			// when
			Optional<User> found = userRepository.findByOauthProviderAndOauthId("NAVER", "naver999");

			// then
			assertThat(found).isEmpty();
		}

		@Test
		@DisplayName("ID는 맞지만 Provider가 다른 경우")
		void findByOauthProviderAndOauthId_WrongProvider_ReturnsEmpty() {
			// given
			userRepository.save(activeUser);

			// when
			Optional<User> found = userRepository.findByOauthProviderAndOauthId("KAKAO", "naver123");

			// then
			assertThat(found).isEmpty();
		}

		@Test
		@DisplayName("여러 Provider의 사용자 중 정확히 찾기")
		void findByOauthProviderAndOauthId_MultipleProviders_FindsCorrectOne() {
			// given
			userRepository.save(activeUser); // NAVER
			userRepository.save(pendingUser); // KAKAO

			// when
			Optional<User> foundNaver = userRepository.findByOauthProviderAndOauthId("NAVER", "naver123");
			Optional<User> foundKakao = userRepository.findByOauthProviderAndOauthId("KAKAO", "kakao456");

			// then
			assertThat(foundNaver).isPresent();
			assertThat(foundNaver.get().getOauthProvider()).isEqualTo("NAVER");

			assertThat(foundKakao).isPresent();
			assertThat(foundKakao.get().getOauthProvider()).isEqualTo("KAKAO");
		}
	}

	@Nested
	@DisplayName("findByShareUuid 테스트")
	class FindByShareUuid {

		@Test
		@DisplayName("Share UUID로 사용자 조회 성공")
		void findByShareUuid_Success() {
			// given
			userRepository.save(activeUser);

			// when
			Optional<User> found = userRepository.findByShareUuid("test-uuid-123");

			// then
			assertThat(found).isPresent();
			assertThat(found.get().getShareUuid()).isEqualTo("test-uuid-123");
			assertThat(found.get().getName()).isEqualTo("테스트");
		}

		@Test
		@DisplayName("존재하지 않는 UUID 조회 시 빈 Optional 반환")
		void findByShareUuid_NotFound_ReturnsEmpty() {
			// when
			Optional<User> found = userRepository.findByShareUuid("nonexistent-uuid");

			// then
			assertThat(found).isEmpty();
		}

		@Test
		@DisplayName("PENDING 상태 사용자는 UUID가 null")
		void findByShareUuid_PendingUser_HasNullUuid() {
			// given
			User saved = userRepository.save(pendingUser);

			// when & then
			assertThat(saved.getShareUuid()).isNull(); // PENDING 상태는 UUID가 null

			// UUID로 조회 불가
			Optional<User> found = userRepository.findByShareUuid("any-uuid");
			assertThat(found).isEmpty();
		}

		@Test
		@DisplayName("UUID는 unique해야 함")
		void findByShareUuid_Unique() {
			// given
			userRepository.save(activeUser);

			User anotherUser = User.builder()
				.email("another@example.com")
				.name("다른사용자")
				.oauthProvider("KAKAO")
				.oauthId("kakao999")
				.selectedColor("blue")
				.shareUuid("test-uuid-123") // 중복 UUID
				.status(UserStatus.ACTIVE)
				.build();

			// when & then
			assertThatThrownBy(() -> userRepository.save(anotherUser))
				.isInstanceOf(Exception.class); // Unique constraint violation
		}

		@Test
		@DisplayName("여러 사용자 중 UUID로 정확히 찾기")
		void findByShareUuid_MultipleUsers_FindsCorrectOne() {
			// given
			User user1 = User.builder()
				.email("user1@example.com")
				.name("사용자1")
				.oauthProvider("NAVER")
				.oauthId("naver111")
				.selectedColor("red")
				.shareUuid("uuid-111")
				.status(UserStatus.ACTIVE)
				.build();

			User user2 = User.builder()
				.email("user2@example.com")
				.name("사용자2")
				.oauthProvider("KAKAO")
				.oauthId("kakao222")
				.selectedColor("blue")
				.shareUuid("uuid-222")
				.status(UserStatus.ACTIVE)
				.build();

			userRepository.save(user1);
			userRepository.save(user2);

			// when
			Optional<User> found = userRepository.findByShareUuid("uuid-222");

			// then
			assertThat(found).isPresent();
			assertThat(found.get().getName()).isEqualTo("사용자2");
			assertThat(found.get().getShareUuid()).isEqualTo("uuid-222");
		}
	}

	@Nested
	@DisplayName("BaseEntity 필드 테스트")
	class BaseEntityFields {

		@Test
		@DisplayName("저장 시 createdAt과 updatedAt 자동 설정")
		void save_SetsTimestamps() {
			// given & when
			User saved = userRepository.save(activeUser);

			// then
			assertThat(saved.getCreatedAt()).isNotNull();
			assertThat(saved.getUpdatedAt()).isNotNull();
		}

		@Test
		@DisplayName("수정 시 updatedAt만 변경")
		void update_UpdatesOnlyUpdatedAt() throws InterruptedException {
			// given
			User saved = userRepository.save(activeUser);
			userRepository.flush();

			var originalCreatedAt = saved.getCreatedAt();
			var originalUpdatedAt = saved.getUpdatedAt();

			Thread.sleep(1000); // 1초 대기로 충분한 시간차 확보

			// when
			saved.setName("수정된이름");
			User updated = userRepository.save(saved);
			userRepository.flush();

			// then
			assertThat(updated.getCreatedAt()).isEqualTo(originalCreatedAt); // 변경 안됨
			assertThat(updated.getUpdatedAt()).isAfter(originalUpdatedAt); // 변경됨
		}
	}

	@Nested
	@DisplayName("기타 기능 테스트")
	class MiscellaneousTests {

		@Test
		@DisplayName("사용자 삭제 성공")
		void deleteUser_Success() {
			// given
			User saved = userRepository.save(activeUser);
			Long userId = saved.getId();

			// when
			userRepository.delete(saved);

			// then
			Optional<User> found = userRepository.findById(userId);
			assertThat(found).isEmpty();
		}

		@Test
		@DisplayName("전체 사용자 수 조회")
		void countUsers_Success() {
			// given
			userRepository.save(activeUser);
			userRepository.save(pendingUser);

			// when
			long count = userRepository.count();

			// then
			assertThat(count).isEqualTo(2);
		}

		@Test
		@DisplayName("ID로 사용자 존재 여부 확인")
		void existsById_Success() {
			// given
			User saved = userRepository.save(activeUser);

			// when
			boolean exists = userRepository.existsById(saved.getId());
			boolean notExists = userRepository.existsById(999L);

			// then
			assertThat(exists).isTrue();
			assertThat(notExists).isFalse();
		}
	}
}
