package com.example.ootd.domain.feed.service.impl;

import com.example.ootd.domain.clothes.entity.Clothes;
import com.example.ootd.domain.clothes.repository.ClothesRepository;
import com.example.ootd.domain.feed.dto.data.CommentDto;
import com.example.ootd.domain.feed.dto.data.FeedDto;
import com.example.ootd.domain.feed.dto.request.CommentCreateRequest;
import com.example.ootd.domain.feed.dto.request.FeedCommentSearchCondition;
import com.example.ootd.domain.feed.dto.request.FeedCreateRequest;
import com.example.ootd.domain.feed.dto.request.FeedSearchCondition;
import com.example.ootd.domain.feed.dto.request.FeedUpdateRequest;
import com.example.ootd.domain.feed.entity.Feed;
import com.example.ootd.domain.feed.entity.FeedClothes;
import com.example.ootd.domain.feed.entity.FeedComment;
import com.example.ootd.domain.feed.entity.FeedLike;
import com.example.ootd.domain.feed.mapper.CommentMapper;
import com.example.ootd.domain.feed.mapper.FeedMapper;
import com.example.ootd.domain.feed.repository.FeedCommentRepository;
import com.example.ootd.domain.feed.repository.FeedLikeRepository;
import com.example.ootd.domain.feed.repository.FeedRepository;
import com.example.ootd.domain.feed.service.FeedService;
import com.example.ootd.domain.user.User;
import com.example.ootd.domain.user.repository.UserRepository;
import com.example.ootd.domain.weather.entity.Weather;
import com.example.ootd.domain.weather.repository.WeatherRepository;
import com.example.ootd.dto.PageResponse;
import com.example.ootd.exception.feed.FeedLikeNotFoundException;
import com.example.ootd.exception.feed.FeedNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {

  private final FeedRepository feedRepository;
  private final FeedLikeRepository feedLikeRepository;
  private final FeedCommentRepository feedCommentRepository;
  private final UserRepository userRepository;
  private final WeatherRepository weatherRepository;
  private final ClothesRepository clothesRepository;
  private final FeedMapper feedMapper;
  private final CommentMapper commentMapper;

  @Override
  public FeedDto createFeed(FeedCreateRequest request) {

    log.debug("피드 등록 시작: {}", request);

    User author = userRepository.findById(request.authorId())
        .orElseThrow(); // TODO: null 예외처리

    Weather weather = weatherRepository.findById(request.weatherId())
        .orElseThrow(); // TODO: null 예외처리

    List<Clothes> clothesList = clothesRepository.findAllById(request.clothesIds());

    Feed feed = Feed.builder()
        .user(author)
        .weather(weather)
        .content(request.content())
        .build();

    for (Clothes clothes : clothesList) {
      FeedClothes feedClothes = FeedClothes.builder()
          .feed(feed)
          .clothes(clothes)
          .build();
      feed.addFeedClothes(feedClothes);
    }

    feedRepository.save(feed);

    FeedDto dto = feedMapper.toDto(feed, false);

    log.info("피드 등록 완료: {}", dto);

    return dto;
  }

  @Override
  public FeedDto updateFeed(UUID feedId, FeedUpdateRequest request, UUID userId) {

    log.debug("피드 수정 시작: feedId={}, {}", feedId, request);

    Feed feed = getFeedById(feedId);
    feed.updateContent(request.content());

    FeedDto dto = feedMapper.toDto(feed, isFeedLiked(feedId, userId));

    log.info("피드 수정 완료: {}", dto);

    return dto;
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponse<FeedDto> findFeedByCondition(FeedSearchCondition condition, UUID userId) {

    log.debug("피드 목록 조회 시작: userId={}, request={}", userId, condition);

    List<Feed> feeds = feedRepository.findByCondition(condition);
    Map<UUID, FeedLike> feedLikeMap = getFeedLikeMapByUserId(userId);

    boolean hasNext = (feeds.size() > condition.limit());
    String nextCursor = null;
    UUID nextIdAfter = null;
    long totalCount = feedRepository.countByCondition(condition);

    // 다음 페이지 있는 경우
    if (hasNext) {
      feeds.remove(feeds.size() - 1);   // 다음 페이지 확인용 마지막 요소 삭제
      Feed lastFeed = feeds.get(feeds.size() - 1);
      nextCursor = getNextCursor(lastFeed, condition.sortBy());
      nextIdAfter = lastFeed.getId();
    }

    List<FeedDto> feedDtos = feedMapper.toDto(feeds, feedLikeMap);

    PageResponse<FeedDto> response = PageResponse.<FeedDto>builder()
        .data(feedDtos)
        .hasNext(hasNext)
        .nextCursor(nextCursor)
        .nextIdAfter(nextIdAfter)
        .sortBy(condition.sortBy())
        .sortDirection(condition.sortDirection())
        .totalCount(totalCount)
        .build();

    log.info("피드 목록 조회 완료: userId={}, feedCount={}", userId, feedDtos.size());

    return response;
  }

  @Override
  public void deleteFeed(UUID feedId) {

    log.debug("피드 삭제 시작: feedId={}", feedId);

    Feed feed = getFeedById(feedId);
    feedRepository.delete(feed);

    log.info("피드 삭제 완료");
  }

  @Override
  public FeedDto likeFeed(UUID feedId, UUID userId) {

    log.debug("피드 좋아요 시작: feedId={}, userId={}", feedId, userId);

    Feed feed = getFeedById(feedId);
    User user = userRepository.findById(userId)
        .orElseThrow(); // TODO: null 예외처리

    FeedLike feedLike = new FeedLike(feed, user);
    feedLikeRepository.save(feedLike);

    feed.increaseLikeCount(); // TODO: 동시성 문제 해결

    FeedDto feedDto = feedMapper.toDto(feed, true);

    log.info("피드 좋아요 완료: {}", feedDto);

    return feedDto;
  }

  @Override
  public void deleteFeedLike(UUID feedId, UUID userId) {

    log.debug("피드 좋아요 삭제 시작: feedId={}, userId={}", feedId, userId);

    Feed feed = getFeedById(feedId);

    FeedLike feedLike = getFeedLikeByFeedIdAndUserId(feedId, userId);
    feedLikeRepository.delete(feedLike);

    feed.decreaseLikeCount(); // TODO: 동시성 문제 해결

    log.info("피드 좋아요 삭제 완료");
  }

  @Override
  public CommentDto createComment(CommentCreateRequest request) {

    log.debug("피드 댓글 등록 시작: {}", request);

    Feed feed = getFeedById(request.feedId());
    User user = userRepository.findById(request.authorId())
        .orElseThrow(); // TODO: null 예외처리

    FeedComment comment = FeedComment.builder()
        .feed(feed)
        .user(user)
        .content(request.content())
        .build();

    feedCommentRepository.save(comment);
    feed.increaseCommentCount();  // TODO: 동시성 문제 해결

    CommentDto commentDto = commentMapper.toDto(comment);

    log.info("피드 댓글 등록 완료: {}", commentDto);

    return commentDto;
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponse<CommentDto> findCommentByCondition(UUID feedId,
      FeedCommentSearchCondition condition) {

    log.debug("피드 댓글 목록 조회 시작: {}", condition);

    List<FeedComment> comments = feedCommentRepository.findByCondition(condition);
    List<CommentDto> commentDtos = commentMapper.toDto(comments);

    boolean hasNext = (comments.size() > condition.limit());
    String nextCursor = null;
    UUID nextIdAfter = null;
    long totalCount = feedCommentRepository.countByFeedId(feedId);

    // 다음 페이지 있는 경우
    if (hasNext) {
      commentDtos.remove(commentDtos.size() - 1);   // 다음 페이지 확인용 마지막 요소 삭제
      CommentDto lastComment = commentDtos.get(commentDtos.size() - 1);
      nextCursor = lastComment.createdAt().toString();
      nextIdAfter = lastComment.id();
    }

    PageResponse<CommentDto> response = PageResponse.<CommentDto>builder()
        .data(commentDtos)
        .hasNext(hasNext)
        .nextCursor(nextCursor)
        .nextIdAfter(nextIdAfter)
        .sortBy("createdAt")
        .sortDirection("ASCENDING")
        .totalCount(totalCount)
        .build();

    log.info("피드 댓글 목록 조회 완료: feedId={}, commentCount={}", feedId, commentDtos.size());

    return response;
  }

  // 피드 엔티티 조회
  private Feed getFeedById(UUID feedId) {
    return feedRepository.findById(feedId)
        .orElseThrow(() -> FeedNotFoundException.withId(feedId));
  }

  // 해당 피드에 좋아요 했는지 여부
  private boolean isFeedLiked(UUID feedId, UUID userId) {

    Optional<FeedLike> feedLike = feedLikeRepository.findByFeedIdAndUserId(feedId, userId);
    return feedLike.isPresent();
  }

  // 피드 좋아요 조회
  private FeedLike getFeedLikeByFeedIdAndUserId(UUID feedId, UUID userId) {
    return feedLikeRepository.findByFeedIdAndUserId(feedId, userId)
        .orElseThrow(() -> FeedLikeNotFoundException.withFeedIdAndUserId(feedId, userId));
  }

  // 좋아요한 피드 전체 조회
  private Map<UUID, FeedLike> getFeedLikeMapByUserId(UUID userId) {

    List<FeedLike> feedLikeList = feedLikeRepository.findAllByUserId(userId);

    if (feedLikeList.isEmpty()) {
      return new HashMap<>();
    }

    // key-feedId, value-feedLike인 Map 반환
    return feedLikeList.stream()
        .collect(Collectors.toMap(
            feedLike -> feedLike.getFeed().getId(),
            Function.identity()
        ));
  }

  // nextCursor 세팅
  private String getNextCursor(Feed feed, String sortBy) {
    switch (sortBy) {
      case "createdAt":
        return feed.getCreatedAt().toString();
      case "likeCount":
        return String.valueOf(feed.getLikeCount());
      default:
        return null;
    }
  }
}