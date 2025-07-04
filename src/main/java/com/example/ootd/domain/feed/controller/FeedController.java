package com.example.ootd.domain.feed.controller;

import com.example.ootd.domain.feed.dto.data.CommentDto;
import com.example.ootd.domain.feed.dto.data.FeedDto;
import com.example.ootd.domain.feed.dto.request.CommentCreateRequest;
import com.example.ootd.domain.feed.dto.request.FeedCommentSearchCondition;
import com.example.ootd.domain.feed.dto.request.FeedCreateRequest;
import com.example.ootd.domain.feed.dto.request.FeedSearchCondition;
import com.example.ootd.domain.feed.dto.request.FeedUpdateRequest;
import com.example.ootd.domain.feed.service.FeedService;
import com.example.ootd.dto.PageResponse;
import com.example.ootd.security.CustomUserDetails;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/api/feeds")
public class FeedController {

  private final FeedService feedService;

  /**
   * 피드
   */
  @GetMapping
  public ResponseEntity<PageResponse<FeedDto>> findFeed(
      @ModelAttribute @Valid FeedSearchCondition condition,
      @AuthenticationPrincipal CustomUserDetails userDetails
  ) {

    UUID userId = userDetails.getUser().getId();

    log.info("피드 목록 조회 요청: userId={}, request={}", userId, condition);

    PageResponse<FeedDto> response = feedService.findFeedByCondition(condition, userId);

    log.debug("피드 목록 조회 응답: feedCount={}", response.data().size());

    return ResponseEntity
        .status(HttpStatus.OK)
        .body(response);
  }

  @PostMapping
  public ResponseEntity<FeedDto> createFeed(@RequestBody FeedCreateRequest request) {

    log.info("피드 등록 요청: {}", request);

    FeedDto response = feedService.createFeed(request);

    log.debug("피드 등록 응답: {}", response);

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(response);
  }

  @DeleteMapping(path = "/{feedId}")
  public ResponseEntity<Void> deleteFeed(@PathVariable UUID feedId) {

    log.info("피드 삭제 요청: feedId={}", feedId);

    feedService.deleteFeed(feedId);

    log.debug("피드 삭제 완료 응답");

    return ResponseEntity
        .status(HttpStatus.NO_CONTENT)
        .build();
  }

  @PatchMapping(path = "/{feedId}")
  public ResponseEntity<FeedDto> updateFeed(
      @PathVariable UUID feedId,
      @RequestBody FeedUpdateRequest request,
      @AuthenticationPrincipal CustomUserDetails userDetails
  ) {

    UUID userId = userDetails.getUser().getId();

    log.info("피드 수정 요청: feedId={}, userId={}, request={}", feedId, userId, request);

    FeedDto response = feedService.updateFeed(feedId, request, userId);

    log.debug("피드 수정 응답: {}", response);

    return ResponseEntity
        .status(HttpStatus.OK)
        .body(response);
  }

  /**
   * 피드 좋아요
   */
  @PostMapping(path = "/{feedId}/like")
  public ResponseEntity<FeedDto> createLike(
      @PathVariable UUID feedId,
      @AuthenticationPrincipal CustomUserDetails userDetails
  ) {

    UUID userId = userDetails.getUser().getId();

    log.info("피드 좋아요 등록 요청: feedId={}, userId={}", feedId, userId);

    FeedDto response = feedService.likeFeed(feedId, userId);

    log.debug("피드 좋아요 등록 응답: {}", response);

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(response);
  }

  @DeleteMapping(path = "/{feedId}/like")
  public ResponseEntity<FeedDto> deleteLike(
      @PathVariable UUID feedId,
      @AuthenticationPrincipal CustomUserDetails userDetails
  ) {

    UUID userId = userDetails.getUser().getId();

    log.info("피드 좋아요 삭제 요청: feedId={}, userId={}", feedId, userId);

    feedService.deleteFeedLike(feedId, userId);

    log.debug("피드 좋아요 삭제 완료");

    return ResponseEntity
        .status(HttpStatus.NO_CONTENT)
        .build();
  }

  /**
   * 피드 댓글
   */
  @GetMapping(path = "/{feedId}/comments")
  public ResponseEntity<PageResponse<CommentDto>> findComment(
      @PathVariable UUID feedId,
      @ModelAttribute FeedCommentSearchCondition condition
  ) {

    log.info("피드 댓글 목록 조회 요청: feedId={}, request={}", feedId, condition);

    PageResponse<CommentDto> response = feedService.findCommentByCondition(feedId, condition);

    log.debug("피드 댓글 목록 조회 응답: commentCount={}", response.data().size());

    return ResponseEntity
        .status(HttpStatus.OK)
        .body(response);
  }

  @PostMapping(path = "/{feedId}/comments")
  public ResponseEntity<CommentDto> createComment(@RequestBody CommentCreateRequest request) {

    log.info("피드 댓글 등록 요청: {}", request);

    CommentDto response = feedService.createComment(request);

    log.debug("피드 댓글 등록 응답: {}", response);

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(response);
  }
}
