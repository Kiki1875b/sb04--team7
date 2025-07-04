//package com.example.ootd.domain.follow.service;
//
//
//import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
//import static org.junit.jupiter.api.Assertions.assertThrows;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.BDDMockito.given;
//import static org.mockito.Mockito.lenient;
//
//import com.example.ootd.domain.follow.dto.FollowCreateRequest;
//import com.example.ootd.domain.follow.dto.FollowDto;
//import com.example.ootd.domain.follow.dto.FollowSummaryDto;
//import com.example.ootd.domain.follow.entity.Follow;
//import com.example.ootd.domain.follow.mapper.FollowMapper;
//import com.example.ootd.domain.follow.repository.FollowRepository;
//import com.example.ootd.domain.follow.service.impl.FollowServiceImpl;
//import com.example.ootd.domain.location.Location;
//import com.example.ootd.domain.location.repository.LocationRepository;
//import com.example.ootd.domain.user.User;
//import com.example.ootd.domain.user.dto.UserSummary;
//import com.example.ootd.domain.user.repository.UserRepository;
//import com.example.ootd.exception.ErrorCode;
//import com.example.ootd.exception.OotdException;
//import java.util.List;
//import java.util.Locale;
//import java.util.Optional;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.util.UUID;
//import org.mockito.junit.jupiter.MockitoSettings;
//import org.mockito.quality.Strictness;
//
//@ExtendWith(MockitoExtension.class)
//@DisplayName("FollowService 테스트")
//@MockitoSettings(strictness = Strictness.LENIENT)
//public class FollowServiceImplTest {
//
//    @Mock
//    private FollowRepository followRepository;
//
//    @Mock
//    private FollowMapper followMapper;
//
//    @Mock
//    private UserRepository userRepository;
//
//    @InjectMocks
//    private FollowServiceImpl followService;
//
//    private User follower;
//    private User followee;
//    private Follow follow;
//    private FollowDto followDto;
//    private FollowCreateRequest followCreateRequest;
//    private FollowSummaryDto followSummaryDto;
//    private UUID followerId;
//    private UUID followeeId;
//    private UUID followId;
//    private Location testLocation;
//
//    @BeforeEach
//    void setUp() {
//        // UUID 생성
//        followerId = UUID.randomUUID();
//        followeeId = UUID.randomUUID();
//        followId = UUID.randomUUID();
//
//        // 테스트용 위치 생성
//        this.testLocation = new Location(37.5665, 126.9780, 60, 127, List.of("서울특별시", "중구", "명동"));
//
//        // User 엔티티 생성
//        this.follower = new User("팔로워", "follower@test.com", "qwer1234", this.testLocation);
//        this.followee = new User("팔로위", "followee@test.com", "qwer1234", this.testLocation);
//
//        // Follow 엔티티 생성
//        follow = Follow.builder()
//                .id(followId)
//                .follower(this.follower)
//                .followee(this.followee)
//                .build();
//
//        // UserSummary 생성
//        UserSummary followerSummary = new UserSummary(followerId, "팔로워", null);
//        UserSummary followeeSummary = new UserSummary(followeeId, "팔로위", null);
//
//        // DTO 생성
//        followDto = FollowDto.builder()
//                .id(followId)
//                .follower(followerSummary)
//                .followee(followeeSummary)
//                .build();
//
//        followCreateRequest = new FollowCreateRequest(followerId, followeeId);
//
//        followSummaryDto = FollowSummaryDto.builder()
//                .followeeId(followeeId)
//                .followerCount(1L)
//                .followingCount(1L)
//                .followedByMe(false)
//                .followedByMeId(followerId)
//                .followingByMe(false)
//                .build();
//    }
//
//    @Nested
//    @DisplayName("팔로우 생성 테스트")
//    class CreateFollowTests {
//
//        @Test
//        @DisplayName("팔로우 생성 성공")
//        void createFollowSuccess() {
//            // given
//            given(userRepository.findById(followerId)).willReturn(Optional.of(follower));
//            given(userRepository.findById(followeeId)).willReturn(Optional.of(followee));
//            given(followRepository.save(any(Follow.class))).willReturn(follow);
//            given(followMapper.toDto(any(Follow.class))).willReturn(followDto);
//
//            // when
//            FollowDto result = followService.createFollow(followCreateRequest);
//
//            // then
//            assertThat(result).isNotNull();
//            assertThat(result.followee().userId()).isEqualTo(followeeId);
//            assertThat(result.follower().userId()).isEqualTo(followerId);
//        }
//
//        @Test
//        @DisplayName("팔로우 생성 실패 - 이미 팔로우 중")
//        void createFollowAlready() {
//            // given
//            given(followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId)).willReturn(true);
//
//            // then
//            assertThrows(OotdException.class, () -> followService.createFollow(followCreateRequest));
//        }
//
//        @Test
//        @DisplayName("팔로우 생성 실패 - 팔로워를 찾을 수 없음")
//        void createFollow_FollowerNotFound() {
//            // given
//            given(userRepository.findById(followerId)).willReturn(Optional.empty());
//
//            // then
//            assertThrows(OotdException.class, () -> followService.createFollow(followCreateRequest));
//        }
//
//        @Test
//        @DisplayName("팔로우 생성 실패 - 팔로우 대상 사용자를 찾을 수 없음")
//        void createFollow_FolloweeNotFound() {
//            // given
//            given(userRepository.findById(followerId)).willReturn(Optional.of(follower));
//            given(userRepository.findById(followeeId)).willReturn(Optional.empty());
//
//            // then
//            assertThrows(OotdException.class, () -> followService.createFollow(followCreateRequest));
//        }
//    }
//
//    @Nested
//    @DisplayName("팔로우 요약 조회 테스트")
//    class GetSummaryFollowTests {
//
//        @Test
//        @DisplayName("팔로우 요약 조회 성공")
//        void getSummaryFollowSuccess() {
//            // given
//            given(userRepository.findById(any(UUID.class))).willReturn(Optional.of(follower));
//            given(followRepository.countByFolloweeId(any(UUID.class))).willReturn(1L);
//            given(followRepository.countByFollowerId(any(UUID.class))).willReturn(1L);
//            given(followRepository.existsByFollowerIdAndFolloweeId(any(UUID.class), any(UUID.class))).willReturn(false);
//
//            // when
//            FollowSummaryDto result = followService.getSummaryFollow(followerId);
//
//            // then
//            assertThat(result).isNotNull();
//        }
//
//        @Test
//        @DisplayName("팔로우 요약 조회 실패 - 유저를 찾을 수 없음")
//        void getSummaryFollow_UserNotFound() {
//            // given
//            given(userRepository.findById(followerId)).willReturn(Optional.empty());
//
//            // then
//            assertThrows(OotdException.class, () -> followService.getSummaryFollow(followerId));
//        }
//    }
//
//    @Nested
//    @DisplayName("팔로우 삭제 테스트")
//    class DeleteFollowTest {
//
//        @Test
//        @DisplayName("팔로우 삭제 성공")
//        void deleteFollowSuccess() {
//            // given
//            given(followRepository.findById(followId)).willReturn(Optional.of(follow));
//
//            // when
//            followService.deleteFollow(followId);
//
//            // then - 삭제가 성공적으로 호출되었는지만 확인
//            // deleteFollow 메서드가 예외 없이 완료되면 성공
//        }
//
//        @Test
//        @DisplayName("팔로우 삭제 실패 - 팔로우를 찾을 수 없음")
//        void deleteFollow_NotFount() {
//            // given
//            given(followRepository.findById(followId)).willReturn(Optional.empty());
//
//            // then
//            assertThrows(OotdException.class, () -> followService.deleteFollow(followId));
//        }
//    }
//}
