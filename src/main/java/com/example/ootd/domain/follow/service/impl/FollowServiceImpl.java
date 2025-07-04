package com.example.ootd.domain.follow.service.impl;


import static com.example.ootd.exception.ErrorCode.ALREADY_FOLLOWED_USER;
import static com.example.ootd.exception.ErrorCode.FOLLOWEE_NOT_FOUND;
import static com.example.ootd.exception.ErrorCode.FOLLOWER_NOT_FOUND;
import static com.example.ootd.exception.ErrorCode.FOLLOW_NOT_FOUND;
import static com.example.ootd.exception.ErrorCode.FOLLOW_USER_NOT_FOUND;

import com.example.ootd.domain.follow.dto.Direction;
import com.example.ootd.domain.follow.dto.FollowCreateRequest;
import com.example.ootd.domain.follow.dto.FollowDto;
import com.example.ootd.domain.follow.dto.FollowListCondition;
import com.example.ootd.domain.follow.dto.FollowListResponse;
import com.example.ootd.domain.follow.dto.FollowSummaryDto;
import com.example.ootd.domain.follow.entity.Follow;
import com.example.ootd.domain.follow.mapper.FollowMapper;
import com.example.ootd.domain.follow.repository.FollowRepository;
import com.example.ootd.domain.follow.service.FollowService;
import com.example.ootd.domain.user.User;
import com.example.ootd.domain.user.repository.UserRepository;
import com.example.ootd.exception.OotdException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FollowServiceImpl implements FollowService {

  private final FollowRepository followRepository;
  private final UserRepository userRepository;
  private final FollowMapper followMapper;

  /**
   * 팔로우 생성
   * @param request follower and followee IDs
   * @return FollowDto
   */
  @Override
  @Transactional
  public FollowDto createFollow(FollowCreateRequest request) {
    log.info("팔로워 생성 : followerId: {}, followeeId: {}", request.followerId(), request.followeeId());

    // 이미 팔로우 하는지 확인
    if (followRepository.existsByFollowerIdAndFolloweeId(request.followerId(), request.followeeId())) {
      throw new OotdException(ALREADY_FOLLOWED_USER);
    }

    // 팔로워와 팔로우 대상 사용자를 조회
    User follower = userRepository.findById(request.followerId())
        .orElseThrow(() -> new OotdException(FOLLOWER_NOT_FOUND));

    User followee = userRepository.findById(request.followeeId())
        .orElseThrow(() -> new OotdException(FOLLOWEE_NOT_FOUND));


    // 팔로우 관계 생성
    Follow follow = Follow.builder()
        .follower(follower)
        .followee(followee)
        .build();

    Follow savedFollow = followRepository.save(follow);

    return followMapper.toDto(savedFollow);
  }

  /**
   * 팔로우 요약 정보 조회
   * @param userId
   * @return FollowSummaryDto
   */
  @Override
  public FollowSummaryDto getSummaryFollow(UUID userId) {
    log.debug("팔로우 요약 조회 : userId: {}", userId);

    // 유저 조회
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new OotdException(FOLLOW_USER_NOT_FOUND));

    // 팔로우 관계 요약 생성
    // MapStruct로 변경하려 했으나 DB 조회가 필요하여 직접 생성
    FollowSummaryDto summary = FollowSummaryDto.builder()
        .followeeId(user.getId())
        .followerCount(followRepository.countByFolloweeId(user.getId()))
        .followingCount(followRepository.countByFollowerId(user.getId()))
        .followingByMe(followRepository.existsByFollowerIdAndFolloweeId(user.getId(), userId))
        .followedByMeId(userId)
        .followedByMe(followRepository.existsByFollowerIdAndFolloweeId(userId, user.getId()))
        .build();


    return summary;
  }

  /**
   * 팔로워 목록 조회
   * @param condition
   * @param followeeId
   * @return FollowListResponse
   */
  @Override
  public FollowListResponse getFollowerList(FollowListCondition condition, UUID followeeId) {
    log.debug("팔로워 목록 조회 : followeeId: {}", followeeId);

    // 값 추출
    String cursor = condition.cursor();
    UUID idAfter = condition.idAfter();
    int limit = condition.limit();
    String nameLike = condition.nameLike();
    String orderBy = condition.orderBy();
    Direction direction = condition.direction();

    // limit + 1로 조회하여 다음 페이지 존재 여부 확인
    List<Follow> followers = followRepository.findFollowersWithCursor(
        followeeId, cursor, idAfter, limit + 1, nameLike, orderBy, direction);

    boolean hasNext = followers.size() > limit;
    List<Follow> responseList = hasNext ? followers.subList(0, limit) : followers;

    String nextCursor = null;
    UUID nextIdAfter = null;
    if (hasNext && !responseList.isEmpty()) {
      Follow lastItem = responseList.get(responseList.size() - 1);
      nextCursor = lastItem.getId().toString();
      nextIdAfter = lastItem.getId();
    }

    // 전체 카운트 조회
    int totalCount = (int) followRepository.countByFolloweeId(followeeId);

    log.debug("팔로워 목록 조회 완료 : followeeId: {}, cursor: {}, hasNext: {}", followeeId, nextCursor, hasNext);

    return FollowListResponse.builder()
        .data(responseList.stream().map(followMapper::toDto).toList())
        .nextCursor(nextCursor)
        .nextIdAfter(nextIdAfter)
        .hasNext(hasNext)
        .totalCount(totalCount)
        .sortBy(orderBy)
        .sortDirection(direction)
        .build();
  }

  /**
   * 팔로잉 목록 조회
   * @param condition
   * @param followerId
   * @return FollowListResponse
   */
  @Override
  public FollowListResponse getFollowingList(FollowListCondition condition, UUID followerId) {
    log.debug("팔로잉 목록 조회 : followerId: {}", followerId);

    // 값 추출
    String cursor = condition.cursor();
    UUID idAfter = condition.idAfter();
    int limit = condition.limit();
    String nameLike = condition.nameLike();
    String orderBy = condition.orderBy();
    Direction direction = condition.direction();

    // limit + 1로 조회하여 다음 페이지 존재 여부 확인
    List<Follow> followees = followRepository.findFollowingsWithCursor(
        followerId, cursor, idAfter, limit + 1, nameLike, orderBy, direction);

    boolean hasNext = followees.size() > limit;
    List<Follow> responseList = hasNext ? followees.subList(0, limit) : followees;

    String nextCursor = null;
    UUID nextIdAfter = null;
    if (hasNext && !responseList.isEmpty()) {
      Follow lastItem = responseList.get(responseList.size() - 1);
      nextCursor = lastItem.getId().toString();
      nextIdAfter = lastItem.getId();
    }

    // 전체 카운트 조회
    int totalCount = (int) followRepository.countByFollowerId(followerId);

    log.debug("팔로잉 목록 조회 완료 : followerId: {}, cursor: {}, hasNext: {}", followerId, nextCursor, hasNext);

    return FollowListResponse.builder()
        .data(responseList.stream().map(followMapper::toDto).toList())
        .nextCursor(nextCursor)
        .nextIdAfter(nextIdAfter)
        .hasNext(hasNext)
        .totalCount(totalCount)
        .sortBy(orderBy)
        .sortDirection(direction)
        .build();
  }

  /**
   * 팔로우 취소
   * @param followId
   */
  @Override
  @Transactional
  public void deleteFollow(UUID followId) {
    log.info("팔로우 취소 : followId: {}", followId);

    // 팔로우 조회
    Follow follow = followRepository.findById(followId)
        .orElseThrow(() -> new OotdException(FOLLOW_NOT_FOUND));

    // 팔로우 취소
    followRepository.delete(follow);
  }
}
