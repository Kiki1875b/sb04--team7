//package com.example.ootd.domain.follow.repository;
//
//import com.example.ootd.config.QueryDslConfig;
//import com.example.ootd.domain.follow.dto.Direction;
//import com.example.ootd.domain.follow.entity.Follow;
//import com.example.ootd.domain.location.Location;
//import com.example.ootd.domain.location.repository.LocationRepository;
//import com.example.ootd.domain.user.User;
//import com.example.ootd.domain.user.repository.UserRepository;
//import java.util.UUID;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
//
//
//import java.util.List;
//import java.util.Optional;
//import org.springframework.context.annotation.Import;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.transaction.annotation.Transactional;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//@DataJpaTest
//@DisplayName("FollowRepository 테스트")
//@Import(QueryDslConfig.class)
//@ActiveProfiles("test")
//@Transactional
//class FollowRepositoryTest {
//
//
//    @Autowired
//    private FollowRepository followRepository;
//
//    @Autowired
//    private UserRepository userRepository;
//
//    @Autowired
//    private LocationRepository locationRepository;
//
//    private User follower;
//    private User followee;
//    private Location testLocation;
//
//    @BeforeEach
//    void setUp() {
//        // 테스트용 위치 생성
//        this.testLocation = new Location(37.5665, 126.9780, 60, 127, List.of("서울특별시", "중구", "명동"));
//        locationRepository.save(this.testLocation);
//
//        // 테스트용 사용자 생성
//        this.follower = new User("팔로워", "follower@test.com", "qwer12314", this.testLocation);
//        this.followee = new User("팔로위", "followee@test.com", "qwer1234", this.testLocation);
//
//        userRepository.save(this.follower);
//        userRepository.save(this.followee);
//    }
//
//    @Nested
//    @DisplayName("JPA Repository 테스트")
//    class JpaRepositoryTests {
//
//        @Test
//        @DisplayName("팔로우 저장 테스트")
//        void saveFollow() {
//            // given
//            Follow follow = Follow.builder()
//                .follower(follower)
//                .followee(followee)
//                .build();
//
//            // when
//            Follow savedFollow = followRepository.save(follow);
//
//            // then
//            assertThat(savedFollow).isNotNull();
//            assertThat(savedFollow.getId()).isNotNull();
//            assertThat(savedFollow.getFollower()).isEqualTo(follower);
//            assertThat(savedFollow.getFollowee()).isEqualTo(followee);
//            assertThat(savedFollow.getCreatedAt()).isNotNull();
//        }
//
//        @Test
//        @DisplayName("팔로우 단건 조회 테스트")
//        void findFollow() {
//            // given
//            Follow follow = Follow.builder()
//                .follower(follower)
//                .followee(followee)
//                .build();
//            Follow savedFollow = followRepository.save(follow);
//
//            // when
//            Optional<Follow> foundFollow = followRepository.findById(savedFollow.getId());
//
//            // then
//            assertThat(foundFollow).isPresent();
//            assertThat(foundFollow.get().getFollower().getId()).isEqualTo(follower.getId());
//            assertThat(foundFollow.get().getFollowee().getId()).isEqualTo(followee.getId());
//        }
//
//        @Test
//        @DisplayName("전체 팔로우 조회 테스트")
//        void findAllFollows() {
//            // given
//            Follow follow1 = Follow.builder()
//                .follower(follower)
//                .followee(followee)
//                .build();
//
//            User testUser = new User("테스트 유저", "test@test.com", "qwer1234", testLocation);
//            userRepository.save(testUser);
//
//            Follow follow2 = Follow.builder()
//                .follower(testUser)
//                .followee(followee)
//                .build();
//
//            followRepository.save(follow1);
//            followRepository.save(follow2);
//
//            // when
//            List<Follow> follows = followRepository.findAll();
//
//            // then
//            assertThat(follows).hasSize(2);
//        }
//
//        @Test
//        @DisplayName("팔로우 삭제 테스트")
//        void deleteFollow() {
//            // given
//            Follow follow = Follow.builder()
//                .follower(follower)
//                .followee(followee)
//                .build();
//            Follow savedFollow = followRepository.save(follow);
//
//            // when
//            followRepository.delete(savedFollow);
//
//            // then
//            Optional<Follow> deletedFollow = followRepository.findById(savedFollow.getId());
//            assertThat(deletedFollow).isEmpty();
//        }
//
//        @Test
//        @DisplayName("팔로우 수 조회 테스트")
//        void countFollows() {
//            // given
//            Follow follow = Follow.builder()
//                .follower(follower)
//                .followee(followee)
//                .build();
//            followRepository.save(follow);
//
//            // when
//            long count = followRepository.count();
//
//            // then
//            assertThat(count).isEqualTo(1);
//        }
//    }
//    @Test
//    @DisplayName("팔로우 존재 여부 확인 테스트 - 존재하는 경우")
//    void existsByFollowerIdAndFolloweeId_Exists() {
//        // given
//        Follow follow = Follow.builder()
//            .follower(follower)
//            .followee(followee)
//            .build();
//        followRepository.save(follow);
//
//        // when
//        boolean exists = followRepository.existsByFollowerIdAndFolloweeId(
//            follower.getId(), followee.getId());
//
//        // then
//        assertThat(exists).isTrue();
//    }
//
//    @Test
//    @DisplayName("팔로우 존재 여부 확인 테스트 - 존재하지 않는 경우")
//    void existsByFollowerIdAndFolloweeId_NotExists() {
//        // given
//        UUID randomFollowerId = UUID.randomUUID();
//        UUID randomFolloweeId = UUID.randomUUID();
//
//        // when
//        boolean exists = followRepository.existsByFollowerIdAndFolloweeId(
//            randomFollowerId, randomFolloweeId);
//
//        // then
//        assertThat(exists).isFalse();
//    }
//
//    @Test
//    @DisplayName("팔로워 수 조회 테스트")
//    void countByFollowerCount() {
//        // given
//        User anotherFollower = new User("팔로워2", "follower2@test.com", "qwer1234", testLocation);
//        userRepository.save(anotherFollower);
//
//        Follow follow1 = Follow.builder()
//            .follower(follower)
//            .followee(followee)
//            .build();
//
//        Follow follow2 = Follow.builder()
//            .follower(anotherFollower)
//            .followee(followee)
//            .build();
//
//        followRepository.save(follow1);
//        followRepository.save(follow2);
//
//        // when
//        long followerCount = followRepository.countByFollowerId(follower.getId());
//
//        // then
//        assertThat(followerCount).isEqualTo(1);
//    }
//
//    @Test
//    @DisplayName("팔로우 수 조회 테스트")
//    void countByFolloweeCount() {
//        // given
//        User anotherFollowee = new User("팔로위2", "followee2@test.com", "qwer1234", testLocation);
//        userRepository.save(anotherFollowee);
//
//        Follow follow1 = Follow.builder()
//            .follower(follower)
//            .followee(followee)
//            .build();
//
//        Follow follow2 = Follow.builder()
//            .follower(follower)
//            .followee(anotherFollowee)
//            .build();
//
//        followRepository.save(follow1);
//        followRepository.save(follow2);
//
//        // when
//        long followeeCount = followRepository.countByFolloweeId(followee.getId());
//
//        // then
//        assertThat(followeeCount).isEqualTo(1);
//    }
//
//    @Test
//    @DisplayName("여러 팔로우 관계에서 특정 사용자의 팔로워 수 조회")
//    void countByFollowerCount_Followers() {
//        // given
//        User follower2 = new User("팔로워2", "follower2@test.com", "qwer1234", testLocation);
//
//        User follower3 = new User("팔로워3", "follower3@test.com", "qwer1234", testLocation);
//
//        userRepository.save(follower2);
//        userRepository.save(follower3);
//
//        // followee를 팔로우하는 사용자들
//        Follow follow1 = Follow.builder()
//            .follower(follower)
//            .followee(followee)
//            .build();
//
//        Follow follow2 = Follow.builder()
//            .follower(follower2)
//            .followee(followee)
//            .build();
//
//        Follow follow3 = Follow.builder()
//            .follower(follower3)
//            .followee(followee)
//            .build();
//
//        followRepository.save(follow1);
//        followRepository.save(follow2);
//        followRepository.save(follow3);
//
//        // when - follower가 팔로우하는 사람 수 (1명)
//        long followerCount = followRepository.countByFollowerId(follower.getId());
//
//        // then
//        assertThat(followerCount).isEqualTo(1);
//    }
//
//    @Test
//    @DisplayName("여러 팔로우 관계에서 특정 사용자를 팔로우하는 사람 수 조회")
//    void countByFolloweeCount_Followees() {
//        // given
//        User follower2 = new User("팔로워2", "follower2@test.com", "qwer1234", testLocation);
//
//        User follower3 = new User("팔로워3", "follower3@test.com", "qwer1234", testLocation);
//
//        userRepository.save(follower2);
//        userRepository.save(follower3);
//
//        // followee를 팔로우하는 여러 사용자들
//        Follow follow1 = Follow.builder()
//            .follower(follower)
//            .followee(followee)
//            .build();
//
//        Follow follow2 = Follow.builder()
//            .follower(follower2)
//            .followee(followee)
//            .build();
//
//        Follow follow3 = Follow.builder()
//            .follower(follower3)
//            .followee(followee)
//            .build();
//
//        followRepository.save(follow1);
//        followRepository.save(follow2);
//        followRepository.save(follow3);
//
//        // when - followee를 팔로우하는 사람 수 (3명)
//        long followeeCount = followRepository.countByFolloweeId(followee.getId());
//
//        // then
//        assertThat(followeeCount).isEqualTo(3);
//    }
//
//    @DisplayName("커서 페이지네이션")
//    @Nested
//    class CursorPage {
//
//        @Test
//        @DisplayName("팔로잉 목록 커서 페이지네이션 테스트")
//        void findFollowingsWithCursor() {
//            // given
//            User user1 = new User("유저1", "user1@test.com", "qwer1234", testLocation);
//            User user2 = new User("유저2", "user2@test.com", "qwer1234", testLocation);
//            User user3 = new User("유저3", "user3@test.com", "qwer1234", testLocation);
//
//            userRepository.save(user1);
//            userRepository.save(user2);
//            userRepository.save(user3);
//
//            Follow follow1 = Follow.builder().follower(follower).followee(user1).build();
//            Follow follow2 = Follow.builder().follower(follower).followee(user2).build();
//            Follow follow3 = Follow.builder().follower(follower).followee(user3).build();
//
//            followRepository.save(follow1);
//            followRepository.save(follow2);
//            followRepository.save(follow3);
//
//            // when
//            List<Follow> result = followRepository.findFollowingsWithCursor(
//                follower.getId(), null, null, 2, null, "createdAt", Direction.ASCENDING);
//
//            // then
//            assertThat(result).hasSize(2);
//        }
//
//        @Test
//        @DisplayName("팔로워 목록 커서 페이지네이션 테스트")
//        void findFollowersWithCursor() {
//            // given
//            User user1 = new User("유저1", "user1@test.com", "qwer1234", testLocation);
//            User user2 = new User("유저2", "user2@test.com", "qwer1234", testLocation);
//            User user3 = new User("유저3", "user3@test.com", "qwer1234", testLocation);
//
//            userRepository.save(user1);
//            userRepository.save(user2);
//            userRepository.save(user3);
//
//            Follow follow1 = Follow.builder().follower(user1).followee(followee).build();
//            Follow follow2 = Follow.builder().follower(user2).followee(followee).build();
//            Follow follow3 = Follow.builder().follower(user3).followee(followee).build();
//
//            followRepository.save(follow1);
//            followRepository.save(follow2);
//            followRepository.save(follow3);
//
//            // when
//            List<Follow> result = followRepository.findFollowersWithCursor(
//                followee.getId(), null, null, 2, null, "createdAt", Direction.ASCENDING);
//
//            // then
//            assertThat(result).hasSize(2);
//        }
//
//        @Test
//        @DisplayName("이름 검색 조건으로 커서 페이지네이션 테스트")
//        void findFollowingsWithCursorAndNameSearch() {
//            // given
//            User alice = new User("Alice", "alice@test.com", "qwer1234", testLocation);
//            User bob = new User("Bob", "bob@test.com", "qwer1234", testLocation);
//            User charlie = new User("Charlie", "charlie@test.com", "qwer1234", testLocation);
//
//            userRepository.save(alice);
//            userRepository.save(bob);
//            userRepository.save(charlie);
//
//            Follow follow1 = Follow.builder().follower(follower).followee(alice).build();
//            Follow follow2 = Follow.builder().follower(follower).followee(bob).build();
//            Follow follow3 = Follow.builder().follower(follower).followee(charlie).build();
//
//            followRepository.save(follow1);
//            followRepository.save(follow2);
//            followRepository.save(follow3);
//
//            // when
//            List<Follow> result = followRepository.findFollowingsWithCursor(
//                follower.getId(), null, null, 10, "A", "createdAt", Direction.ASCENDING);
//
//            // then
//            assertThat(result).hasSize(1);
//            assertThat(result.get(0).getFollowee().getName()).isEqualTo("Alice");
//        }
//    }
//
//
//}