package com.example.adventcalendar.repository;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.adventcalendar.constant.UserStatus;
import com.example.adventcalendar.entity.Letter;
import com.example.adventcalendar.entity.User;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("LetterRepository 통합 테스트")
class LetterRepositoryTest {

	@Autowired
	private LetterRepository letterRepository;

	@Autowired
	private UserRepository userRepository;

	private User user;
	private Letter letter1;
	private Letter letter2;
	private Letter letter3;

	@BeforeEach
	void setUp() {
		letterRepository.deleteAll();
		userRepository.deleteAll();

		// 테스트용 사용자 생성
		user = User.builder()
			.email("test@example.com")
			.name("테스트")
			.oauthProvider("NAVER")
			.oauthId("naver123")
			.selectedColor("green")
			.shareUuid("test-uuid-123")
			.status(UserStatus.ACTIVE)
			.build();
		user = userRepository.save(user);

		// 테스트용 편지 생성
		letter1 = Letter.builder()
			.user(user)
			.day(1)
			.content("첫 번째 편지")
			.fromName("친구1")
			.build();

		letter2 = Letter.builder()
			.user(user)
			.day(10)
			.content("열 번째 편지")
			.fromName("친구2")
			.build();

		letter3 = Letter.builder()
			.user(user)
			.day(25)
			.content("크리스마스 편지")
			.fromName("산타")
			.build();
	}

	@Nested
	@DisplayName("findByUserId 테스트")
	class FindByUserId {

		@Test
		@DisplayName("사용자 ID로 모든 편지 조회 성공")
		void findByUserId_Success() {
			// given
			letterRepository.save(letter1);
			letterRepository.save(letter2);
			letterRepository.save(letter3);

			// when
			List<Letter> letters = letterRepository.findByUserId(user.getId());

			// then
			assertThat(letters).hasSize(3);
			assertThat(letters).extracting(Letter::getDay)
				.containsExactlyInAnyOrder(1, 10, 25);
		}

		@Test
		@DisplayName("편지가 없는 사용자는 빈 리스트 반환")
		void findByUserId_NoLetters_ReturnsEmptyList() {
			// when
			List<Letter> letters = letterRepository.findByUserId(user.getId());

			// then
			assertThat(letters).isEmpty();
		}

		@Test
		@DisplayName("존재하지 않는 사용자 ID로 조회 시 빈 리스트")
		void findByUserId_NonexistentUser_ReturnsEmptyList() {
			// given
			letterRepository.save(letter1);

			// when
			List<Letter> letters = letterRepository.findByUserId(999L);

			// then
			assertThat(letters).isEmpty();
		}

		@Test
		@DisplayName("여러 사용자의 편지 중 특정 사용자만 조회")
		void findByUserId_MultipleUsers_FindsOnlySpecificUser() {
			// given
			User anotherUser = User.builder()
				.email("another@example.com")
				.name("다른사용자")
				.oauthProvider("KAKAO")
				.oauthId("kakao456")
				.selectedColor("blue")
				.shareUuid("uuid-456")
				.status(UserStatus.ACTIVE)
				.build();
			anotherUser = userRepository.save(anotherUser);

			letterRepository.save(letter1); // user의 편지

			Letter anotherLetter = Letter.builder()
				.user(anotherUser)
				.day(5)
				.content("다른 사용자 편지")
				.fromName("다른친구")
				.build();
			letterRepository.save(anotherLetter);

			// when
			List<Letter> userLetters = letterRepository.findByUserId(user.getId());
			List<Letter> anotherUserLetters = letterRepository.findByUserId(anotherUser.getId());

			// then
			assertThat(userLetters).hasSize(1);
			assertThat(userLetters.get(0).getContent()).isEqualTo("첫 번째 편지");

			assertThat(anotherUserLetters).hasSize(1);
			assertThat(anotherUserLetters.get(0).getContent()).isEqualTo("다른 사용자 편지");
		}
	}

	@Nested
	@DisplayName("findByUserIdAndDayLessThanEqual 테스트")
	class FindByUserIdAndDayLessThanEqual {

		@Test
		@DisplayName("특정 날짜 이하의 편지만 조회")
		void findByUserIdAndDayLessThanEqual_Success() {
			// given
			letterRepository.save(letter1); // day 1
			letterRepository.save(letter2); // day 10
			letterRepository.save(letter3); // day 25

			// when
			List<Letter> letters = letterRepository.findByUserIdAndDayLessThanEqual(user.getId(), 10);

			// then
			assertThat(letters).hasSize(2);
			assertThat(letters).extracting(Letter::getDay)
				.containsExactlyInAnyOrder(1, 10);
			assertThat(letters).extracting(Letter::getContent)
				.doesNotContain("크리스마스 편지");
		}

		@Test
		@DisplayName("day 1로 조회 시 1일 편지만 반환")
		void findByUserIdAndDayLessThanEqual_Day1_ReturnsOnlyDay1() {
			// given
			letterRepository.save(letter1); // day 1
			letterRepository.save(letter2); // day 10

			// when
			List<Letter> letters = letterRepository.findByUserIdAndDayLessThanEqual(user.getId(), 1);

			// then
			assertThat(letters).hasSize(1);
			assertThat(letters.get(0).getDay()).isEqualTo(1);
		}

		@Test
		@DisplayName("day 25로 조회 시 모든 편지 반환")
		void findByUserIdAndDayLessThanEqual_Day25_ReturnsAll() {
			// given
			letterRepository.save(letter1);
			letterRepository.save(letter2);
			letterRepository.save(letter3);

			// when
			List<Letter> letters = letterRepository.findByUserIdAndDayLessThanEqual(user.getId(), 25);

			// then
			assertThat(letters).hasSize(3);
		}

		@Test
		@DisplayName("해당 날짜보다 이전 날짜가 없으면 빈 리스트")
		void findByUserIdAndDayLessThanEqual_NoPreviousDays_ReturnsEmpty() {
			// given
			letterRepository.save(letter2); // day 10
			letterRepository.save(letter3); // day 25

			// when
			List<Letter> letters = letterRepository.findByUserIdAndDayLessThanEqual(user.getId(), 5);

			// then
			assertThat(letters).isEmpty();
		}

		@Test
		@DisplayName("경계값 테스트 - day 정확히 일치하는 경우 포함")
		void findByUserIdAndDayLessThanEqual_ExactMatch_Included() {
			// given
			letterRepository.save(letter2); // day 10

			// when
			List<Letter> letters = letterRepository.findByUserIdAndDayLessThanEqual(user.getId(), 10);

			// then
			assertThat(letters).hasSize(1);
			assertThat(letters.get(0).getDay()).isEqualTo(10);
		}
	}

	@Nested
	@DisplayName("findByUserIdAndDay 테스트")
	class FindByUserIdAndDay {

		@Test
		@DisplayName("특정 날짜의 편지만 조회")
		void findByUserIdAndDay_Success() {
			// given
			letterRepository.save(letter1); // day 1
			letterRepository.save(letter2); // day 10

			// when
			List<Letter> letters = letterRepository.findByUserIdAndDay(user.getId(), 10);

			// then
			assertThat(letters).hasSize(1);
			assertThat(letters.get(0).getDay()).isEqualTo(10);
			assertThat(letters.get(0).getContent()).isEqualTo("열 번째 편지");
		}

		@Test
		@DisplayName("같은 날짜에 여러 편지가 있는 경우 모두 조회")
		void findByUserIdAndDay_MultipleLettersSameDay_ReturnsAll() {
			// given
			Letter sameDayLetter1 = Letter.builder()
				.user(user)
				.day(10)
				.content("첫 번째")
				.fromName("친구1")
				.build();

			Letter sameDayLetter2 = Letter.builder()
				.user(user)
				.day(10)
				.content("두 번째")
				.fromName("친구2")
				.build();

			letterRepository.save(sameDayLetter1);
			letterRepository.save(sameDayLetter2);

			// when
			List<Letter> letters = letterRepository.findByUserIdAndDay(user.getId(), 10);

			// then
			assertThat(letters).hasSize(2);
			assertThat(letters).extracting(Letter::getFromName)
				.containsExactlyInAnyOrder("친구1", "친구2");
		}

		@Test
		@DisplayName("해당 날짜에 편지가 없으면 빈 리스트")
		void findByUserIdAndDay_NoLetters_ReturnsEmpty() {
			// given
			letterRepository.save(letter1); // day 1

			// when
			List<Letter> letters = letterRepository.findByUserIdAndDay(user.getId(), 5);

			// then
			assertThat(letters).isEmpty();
		}

		@Test
		@DisplayName("다른 사용자의 같은 날짜 편지는 조회되지 않음")
		void findByUserIdAndDay_OtherUserLetters_NotIncluded() {
			// given
			User anotherUser = User.builder()
				.email("another@example.com")
				.name("다른사용자")
				.oauthProvider("KAKAO")
				.oauthId("kakao456")
				.selectedColor("blue")
				.shareUuid("uuid-456")
				.status(UserStatus.ACTIVE)
				.build();
			anotherUser = userRepository.save(anotherUser);

			letterRepository.save(letter1); // user의 day 1 편지

			Letter anotherUserLetter = Letter.builder()
				.user(anotherUser)
				.day(1) // 같은 날짜
				.content("다른 사용자 편지")
				.fromName("다른친구")
				.build();
			letterRepository.save(anotherUserLetter);

			// when
			List<Letter> letters = letterRepository.findByUserIdAndDay(user.getId(), 1);

			// then
			assertThat(letters).hasSize(1);
			assertThat(letters.get(0).getContent()).isEqualTo("첫 번째 편지");
		}
	}

	@Nested
	@DisplayName("countByUserIdGroupByDay 테스트")
	class CountByUserIdGroupByDay {

		@Test
		@DisplayName("날짜별 편지 개수 집계 성공")
		void countByUserIdGroupByDay_Success() {
			// given
			letterRepository.save(letter1); // day 1
			letterRepository.save(letter2); // day 10
			letterRepository.save(letter3); // day 25

			// when
			List<Object[]> results = letterRepository.countByUserIdGroupByDay(user.getId());

			// then
			assertThat(results).hasSize(3);

			// 결과 검증
			for (Object[] result : results) {
				Integer day = (Integer) result[0];
				Long count = (Long) result[1];

				assertThat(count).isEqualTo(1L);
				assertThat(day).isIn(1, 10, 25);
			}
		}

		@Test
		@DisplayName("같은 날짜에 여러 편지가 있는 경우 카운트 정확히 집계")
		void countByUserIdGroupByDay_MultipleSameDay_CountsCorrectly() {
			// given
			Letter day10Letter1 = Letter.builder()
				.user(user)
				.day(10)
				.content("첫 번째")
				.fromName("친구1")
				.build();

			Letter day10Letter2 = Letter.builder()
				.user(user)
				.day(10)
				.content("두 번째")
				.fromName("친구2")
				.build();

			Letter day10Letter3 = Letter.builder()
				.user(user)
				.day(10)
				.content("세 번째")
				.fromName("친구3")
				.build();

			letterRepository.save(day10Letter1);
			letterRepository.save(day10Letter2);
			letterRepository.save(day10Letter3);
			letterRepository.save(letter1); // day 1

			// when
			List<Object[]> results = letterRepository.countByUserIdGroupByDay(user.getId());

			// then
			assertThat(results).hasSize(2); // day 1, day 10

			// day 10의 개수 확인
			Object[] day10Result = results.stream()
				.filter(r -> ((Integer) r[0]).equals(10))
				.findFirst()
				.orElseThrow();

			assertThat((Long) day10Result[1]).isEqualTo(3L);
		}

		@Test
		@DisplayName("편지가 없는 사용자는 빈 리스트")
		void countByUserIdGroupByDay_NoLetters_ReturnsEmpty() {
			// when
			List<Object[]> results = letterRepository.countByUserIdGroupByDay(user.getId());

			// then
			assertThat(results).isEmpty();
		}

		@Test
		@DisplayName("다른 사용자의 편지는 집계되지 않음")
		void countByUserIdGroupByDay_OtherUserLetters_NotCounted() {
			// given
			User anotherUser = User.builder()
				.email("another@example.com")
				.name("다른사용자")
				.oauthProvider("KAKAO")
				.oauthId("kakao456")
				.selectedColor("blue")
				.shareUuid("uuid-456")
				.status(UserStatus.ACTIVE)
				.build();
			anotherUser = userRepository.save(anotherUser);

			letterRepository.save(letter1); // user의 day 1 편지

			Letter anotherUserLetter1 = Letter.builder()
				.user(anotherUser)
				.day(1)
				.content("다른 사용자 편지1")
				.fromName("다른친구")
				.build();

			Letter anotherUserLetter2 = Letter.builder()
				.user(anotherUser)
				.day(1)
				.content("다른 사용자 편지2")
				.fromName("다른친구")
				.build();

			letterRepository.save(anotherUserLetter1);
			letterRepository.save(anotherUserLetter2);

			// when
			List<Object[]> results = letterRepository.countByUserIdGroupByDay(user.getId());

			// then
			assertThat(results).hasSize(1);
			assertThat((Long) results.get(0)[1]).isEqualTo(1L); // user는 1개만
		}

		@Test
		@DisplayName("모든 날짜(1-25)에 편지가 있는 경우")
		void countByUserIdGroupByDay_AllDays_CountsAll() {
			// given
			for (int day = 1; day <= 25; day++) {
				Letter letter = Letter.builder()
					.user(user)
					.day(day)
					.content("Day " + day)
					.fromName("친구")
					.build();
				letterRepository.save(letter);
			}

			// when
			List<Object[]> results = letterRepository.countByUserIdGroupByDay(user.getId());

			// then
			assertThat(results).hasSize(25);

			// 모든 날짜가 1개씩 있는지 확인
			for (Object[] result : results) {
				assertThat((Long) result[1]).isEqualTo(1L);
			}
		}
	}

	@Nested
	@DisplayName("BaseEntity 필드 테스트")
	class BaseEntityFields {

		@Test
		@DisplayName("저장 시 createdAt과 updatedAt 자동 설정")
		void save_SetsTimestamps() {
			// given & when
			Letter saved = letterRepository.save(letter1);

			// then
			assertThat(saved.getCreatedAt()).isNotNull();
			assertThat(saved.getUpdatedAt()).isNotNull();
		}

		@Test
		@DisplayName("수정 시 updatedAt만 변경")
		void update_UpdatesOnlyUpdatedAt() throws InterruptedException {
			// given
			Letter saved = letterRepository.save(letter1);
			letterRepository.flush();

			var originalCreatedAt = saved.getCreatedAt();
			var originalUpdatedAt = saved.getUpdatedAt();

			Thread.sleep(1000); // 1초 대기

			// when
			saved.setContent("수정된 내용");
			Letter updated = letterRepository.save(saved);
			letterRepository.flush();

			// then
			assertThat(updated.getCreatedAt()).isEqualTo(originalCreatedAt);
			assertThat(updated.getUpdatedAt()).isAfter(originalUpdatedAt);
		}
	}

	@Nested
	@DisplayName("기타 기능 테스트")
	class MiscellaneousTests {

		@Test
		@DisplayName("편지 삭제 성공")
		void deleteLetter_Success() {
			// given
			Letter saved = letterRepository.save(letter1);
			Long letterId = saved.getId();

			// when
			letterRepository.delete(saved);

			// then
			assertThat(letterRepository.findById(letterId)).isEmpty();
		}

		@Test
		@DisplayName("전체 편지 수 조회")
		void countLetters_Success() {
			// given
			letterRepository.save(letter1);
			letterRepository.save(letter2);
			letterRepository.save(letter3);

			// when
			long count = letterRepository.count();

			// then
			assertThat(count).isEqualTo(3);
		}

		@Test
		@DisplayName("ID로 편지 존재 여부 확인")
		void existsById_Success() {
			// given
			Letter saved = letterRepository.save(letter1);

			// when
			boolean exists = letterRepository.existsById(saved.getId());
			boolean notExists = letterRepository.existsById(999L);

			// then
			assertThat(exists).isTrue();
			assertThat(notExists).isFalse();
		}

		@Test
		@DisplayName("User와의 관계 검증")
		void verifyUserRelationship() {
			// given
			Letter saved = letterRepository.save(letter1);

			// when
			Letter found = letterRepository.findById(saved.getId()).orElseThrow();

			// then
			assertThat(found.getUser()).isNotNull();
			assertThat(found.getUser().getId()).isEqualTo(user.getId());
			assertThat(found.getUser().getEmail()).isEqualTo("test@example.com");
		}
	}
}
