package com.example.adventcalendar.repository;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.adventcalendar.entity.RefreshToken;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("RefreshTokenRepository 통합 테스트")
class RefreshTokenRepositoryTest {

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	private RefreshToken validToken;
	private RefreshToken expiredToken;

	@BeforeEach
	void setUp() {
		refreshTokenRepository.deleteAll();

		validToken = RefreshToken.builder()
			.userId(1L)
			.token("valid-refresh-token-123")
			.expiresAt(LocalDateTime.now().plusDays(7))
			.build();

		expiredToken = RefreshToken.builder()
			.userId(2L)
			.token("expired-refresh-token-456")
			.expiresAt(LocalDateTime.now().minusDays(1))
			.build();
	}

	@Nested
	@DisplayName("findByToken 테스트")
	class FindByToken {

		@Test
		@DisplayName("토큰으로 RefreshToken 조회 성공")
		void findByToken_Success() {
			// given
			refreshTokenRepository.save(validToken);

			// when
			Optional<RefreshToken> found = refreshTokenRepository.findByToken("valid-refresh-token-123");

			// then
			assertThat(found).isPresent();
			assertThat(found.get().getUserId()).isEqualTo(1L);
			assertThat(found.get().getToken()).isEqualTo("valid-refresh-token-123");
			assertThat(found.get().getExpiresAt()).isAfter(LocalDateTime.now());
		}

		@Test
		@DisplayName("존재하지 않는 토큰 조회 시 빈 Optional 반환")
		void findByToken_NotFound_ReturnsEmpty() {
			// when
			Optional<RefreshToken> found = refreshTokenRepository.findByToken("nonexistent-token");

			// then
			assertThat(found).isEmpty();
		}

		@Test
		@DisplayName("만료된 토큰도 조회 가능")
		void findByToken_ExpiredToken_CanBeFound() {
			// given
			refreshTokenRepository.save(expiredToken);

			// when
			Optional<RefreshToken> found = refreshTokenRepository.findByToken("expired-refresh-token-456");

			// then
			assertThat(found).isPresent();
			assertThat(found.get().getExpiresAt()).isBefore(LocalDateTime.now());
		}

		@Test
		@DisplayName("여러 토큰 중 특정 토큰 조회")
		void findByToken_MultipleTokens_FindsCorrectOne() {
			// given
			RefreshToken token1 = RefreshToken.builder()
				.userId(1L)
				.token("token-1")
				.expiresAt(LocalDateTime.now().plusDays(7))
				.build();

			RefreshToken token2 = RefreshToken.builder()
				.userId(2L)
				.token("token-2")
				.expiresAt(LocalDateTime.now().plusDays(7))
				.build();

			refreshTokenRepository.save(token1);
			refreshTokenRepository.save(token2);

			// when
			Optional<RefreshToken> found = refreshTokenRepository.findByToken("token-2");

			// then
			assertThat(found).isPresent();
			assertThat(found.get().getUserId()).isEqualTo(2L);
		}

		@Test
		@DisplayName("토큰은 unique해야 함")
		void findByToken_Unique() {
			// given
			refreshTokenRepository.save(validToken);

			RefreshToken duplicateToken = RefreshToken.builder()
				.userId(3L)
				.token("valid-refresh-token-123") // 중복 토큰
				.expiresAt(LocalDateTime.now().plusDays(7))
				.build();

			// when & then
			assertThatThrownBy(() -> {
				refreshTokenRepository.save(duplicateToken);
				refreshTokenRepository.flush();
			}).isInstanceOf(Exception.class); // Unique constraint violation
		}
	}

	@Nested
	@DisplayName("findByUserId 테스트")
	class FindByUserId {

		@Test
		@DisplayName("사용자 ID로 RefreshToken 조회 성공")
		void findByUserId_Success() {
			// given
			refreshTokenRepository.save(validToken);

			// when
			Optional<RefreshToken> found = refreshTokenRepository.findByUserId(1L);

			// then
			assertThat(found).isPresent();
			assertThat(found.get().getUserId()).isEqualTo(1L);
			assertThat(found.get().getToken()).isEqualTo("valid-refresh-token-123");
		}

		@Test
		@DisplayName("존재하지 않는 사용자 ID 조회 시 빈 Optional 반환")
		void findByUserId_NotFound_ReturnsEmpty() {
			// when
			Optional<RefreshToken> found = refreshTokenRepository.findByUserId(999L);

			// then
			assertThat(found).isEmpty();
		}

		@Test
		@DisplayName("한 사용자는 하나의 RefreshToken만 가짐")
		void findByUserId_OneTokenPerUser() {
			// given
			refreshTokenRepository.save(validToken);

			// when
			Optional<RefreshToken> found = refreshTokenRepository.findByUserId(1L);

			// then
			assertThat(found).isPresent();

			// 같은 userId로 여러 토큰 저장 시도
			RefreshToken newToken = RefreshToken.builder()
				.userId(1L)
				.token("new-token-for-user-1")
				.expiresAt(LocalDateTime.now().plusDays(7))
				.build();

			refreshTokenRepository.save(newToken);

			// 새 토큰이 저장되면 2개가 조회됨 (비즈니스 로직에서 관리 필요)
			long count = refreshTokenRepository.count();
			assertThat(count).isEqualTo(2); // userId는 unique 제약이 없음
		}

		@Test
		@DisplayName("여러 사용자의 토큰 중 특정 사용자만 조회")
		void findByUserId_MultipleUsers_FindsCorrectOne() {
			// given
			refreshTokenRepository.save(validToken); // userId 1
			refreshTokenRepository.save(expiredToken); // userId 2

			// when
			Optional<RefreshToken> found1 = refreshTokenRepository.findByUserId(1L);
			Optional<RefreshToken> found2 = refreshTokenRepository.findByUserId(2L);

			// then
			assertThat(found1).isPresent();
			assertThat(found1.get().getToken()).isEqualTo("valid-refresh-token-123");

			assertThat(found2).isPresent();
			assertThat(found2.get().getToken()).isEqualTo("expired-refresh-token-456");
		}
	}

	@Nested
	@DisplayName("deleteByUserId 테스트")
	class DeleteByUserId {

		@Test
		@DisplayName("사용자 ID로 RefreshToken 삭제 성공")
		void deleteByUserId_Success() {
			// given
			refreshTokenRepository.save(validToken);
			assertThat(refreshTokenRepository.findByUserId(1L)).isPresent();

			// when
			refreshTokenRepository.deleteByUserId(1L);
			refreshTokenRepository.flush();

			// then
			Optional<RefreshToken> found = refreshTokenRepository.findByUserId(1L);
			assertThat(found).isEmpty();
		}

		@Test
		@DisplayName("존재하지 않는 사용자 ID 삭제 시도해도 예외 없음")
		void deleteByUserId_NonexistentUser_NoException() {
			// when & then
			assertThatCode(() -> {
				refreshTokenRepository.deleteByUserId(999L);
				refreshTokenRepository.flush();
			}).doesNotThrowAnyException();
		}

		@Test
		@DisplayName("특정 사용자만 삭제하고 다른 사용자는 유지")
		void deleteByUserId_OnlyDeletesSpecificUser() {
			// given
			refreshTokenRepository.save(validToken); // userId 1
			refreshTokenRepository.save(expiredToken); // userId 2

			// when
			refreshTokenRepository.deleteByUserId(1L);
			refreshTokenRepository.flush();

			// then
			assertThat(refreshTokenRepository.findByUserId(1L)).isEmpty();
			assertThat(refreshTokenRepository.findByUserId(2L)).isPresent();
		}

		@Test
		@DisplayName("같은 userId의 토큰이 여러 개 있어도 모두 삭제")
		void deleteByUserId_DeletesAllTokensForUser() {
			// given
			RefreshToken token1 = RefreshToken.builder()
				.userId(1L)
				.token("token-1")
				.expiresAt(LocalDateTime.now().plusDays(7))
				.build();

			RefreshToken token2 = RefreshToken.builder()
				.userId(1L)
				.token("token-2")
				.expiresAt(LocalDateTime.now().plusDays(7))
				.build();

			refreshTokenRepository.save(token1);
			refreshTokenRepository.save(token2);

			assertThat(refreshTokenRepository.count()).isEqualTo(2);

			// when
			refreshTokenRepository.deleteByUserId(1L);
			refreshTokenRepository.flush();

			// then
			assertThat(refreshTokenRepository.count()).isEqualTo(0);
		}
	}

	@Nested
	@DisplayName("deleteByExpiresAtBefore 테스트")
	class DeleteByExpiresAtBefore {

		@Test
		@DisplayName("특정 시간 이전에 만료된 토큰만 삭제")
		void deleteByExpiresAtBefore_Success() {
			// given
			refreshTokenRepository.save(validToken); // 7일 후 만료
			refreshTokenRepository.save(expiredToken); // 1일 전 만료

			assertThat(refreshTokenRepository.count()).isEqualTo(2);

			// when
			refreshTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
			refreshTokenRepository.flush();

			// then
			assertThat(refreshTokenRepository.count()).isEqualTo(1);
			assertThat(refreshTokenRepository.findByToken("valid-refresh-token-123")).isPresent();
			assertThat(refreshTokenRepository.findByToken("expired-refresh-token-456")).isEmpty();
		}

		@Test
		@DisplayName("만료되지 않은 토큰은 삭제되지 않음")
		void deleteByExpiresAtBefore_ValidTokensNotDeleted() {
			// given
			RefreshToken token1 = RefreshToken.builder()
				.userId(1L)
				.token("token-1")
				.expiresAt(LocalDateTime.now().plusDays(1))
				.build();

			RefreshToken token2 = RefreshToken.builder()
				.userId(2L)
				.token("token-2")
				.expiresAt(LocalDateTime.now().plusDays(7))
				.build();

			refreshTokenRepository.save(token1);
			refreshTokenRepository.save(token2);

			// when
			refreshTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
			refreshTokenRepository.flush();

			// then
			assertThat(refreshTokenRepository.count()).isEqualTo(2); // 모두 유지
		}

		@Test
		@DisplayName("모든 토큰이 만료된 경우 전부 삭제")
		void deleteByExpiresAtBefore_AllExpired_DeletesAll() {
			// given
			RefreshToken token1 = RefreshToken.builder()
				.userId(1L)
				.token("token-1")
				.expiresAt(LocalDateTime.now().minusDays(10))
				.build();

			RefreshToken token2 = RefreshToken.builder()
				.userId(2L)
				.token("token-2")
				.expiresAt(LocalDateTime.now().minusDays(5))
				.build();

			refreshTokenRepository.save(token1);
			refreshTokenRepository.save(token2);

			// when
			refreshTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
			refreshTokenRepository.flush();

			// then
			assertThat(refreshTokenRepository.count()).isEqualTo(0);
		}

		@Test
		@DisplayName("경계값 테스트 - 정확히 같은 시간")
		void deleteByExpiresAtBefore_ExactTime() {
			// given
			LocalDateTime exactTime = LocalDateTime.now();

			RefreshToken exactToken = RefreshToken.builder()
				.userId(1L)
				.token("exact-token")
				.expiresAt(exactTime)
				.build();

			refreshTokenRepository.save(exactToken);

			// when - before는 '<' 이므로 같은 시간은 삭제 안됨
			refreshTokenRepository.deleteByExpiresAtBefore(exactTime);
			refreshTokenRepository.flush();

			// then
			assertThat(refreshTokenRepository.count()).isEqualTo(1); // 삭제 안됨
		}

		@Test
		@DisplayName("만료된 토큰이 없으면 아무것도 삭제되지 않음")
		void deleteByExpiresAtBefore_NoExpiredTokens_NothingDeleted() {
			// given
			refreshTokenRepository.save(validToken);

			// when
			refreshTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
			refreshTokenRepository.flush();

			// then
			assertThat(refreshTokenRepository.count()).isEqualTo(1);
		}
	}

	@Nested
	@DisplayName("BaseEntity 필드 테스트")
	class BaseEntityFields {

		@Test
		@DisplayName("저장 시 createdAt과 updatedAt 자동 설정")
		void save_SetsTimestamps() {
			// given & when
			RefreshToken saved = refreshTokenRepository.save(validToken);

			// then
			assertThat(saved.getCreatedAt()).isNotNull();
			assertThat(saved.getUpdatedAt()).isNotNull();
		}

		@Test
		@DisplayName("수정 시 updatedAt만 변경")
		void update_UpdatesOnlyUpdatedAt() throws InterruptedException {
			// given
			RefreshToken saved = refreshTokenRepository.save(validToken);
			refreshTokenRepository.flush();

			var originalCreatedAt = saved.getCreatedAt();
			var originalUpdatedAt = saved.getUpdatedAt();

			Thread.sleep(1000); // 1초 대기

			// when
			saved.setToken("updated-token");
			RefreshToken updated = refreshTokenRepository.save(saved);
			refreshTokenRepository.flush();

			// then
			assertThat(updated.getCreatedAt()).isEqualTo(originalCreatedAt);
			assertThat(updated.getUpdatedAt()).isAfter(originalUpdatedAt);
		}
	}

	@Nested
	@DisplayName("기타 기능 테스트")
	class MiscellaneousTests {

		@Test
		@DisplayName("RefreshToken 삭제 성공")
		void deleteToken_Success() {
			// given
			RefreshToken saved = refreshTokenRepository.save(validToken);
			Long tokenId = saved.getId();

			// when
			refreshTokenRepository.delete(saved);

			// then
			assertThat(refreshTokenRepository.findById(tokenId)).isEmpty();
		}

		@Test
		@DisplayName("전체 RefreshToken 수 조회")
		void countTokens_Success() {
			// given
			refreshTokenRepository.save(validToken);
			refreshTokenRepository.save(expiredToken);

			// when
			long count = refreshTokenRepository.count();

			// then
			assertThat(count).isEqualTo(2);
		}

		@Test
		@DisplayName("ID로 RefreshToken 존재 여부 확인")
		void existsById_Success() {
			// given
			RefreshToken saved = refreshTokenRepository.save(validToken);

			// when
			boolean exists = refreshTokenRepository.existsById(saved.getId());
			boolean notExists = refreshTokenRepository.existsById(999L);

			// then
			assertThat(exists).isTrue();
			assertThat(notExists).isFalse();
		}

		@Test
		@DisplayName("만료 시간 업데이트")
		void updateExpiresAt_Success() {
			// given
			RefreshToken saved = refreshTokenRepository.save(validToken);
			LocalDateTime originalExpiresAt = saved.getExpiresAt();

			// when
			LocalDateTime newExpiresAt = LocalDateTime.now().plusDays(30);
			saved.setExpiresAt(newExpiresAt);
			RefreshToken updated = refreshTokenRepository.save(saved);

			// then
			assertThat(updated.getExpiresAt()).isEqualTo(newExpiresAt);
			assertThat(updated.getExpiresAt()).isNotEqualTo(originalExpiresAt);
		}
	}
}
