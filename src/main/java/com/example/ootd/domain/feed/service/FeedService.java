package com.example.ootd.domain.feed.service;

import com.example.ootd.domain.feed.dto.data.CommentDto;
import com.example.ootd.domain.feed.dto.data.FeedDto;
import com.example.ootd.domain.feed.dto.request.CommentCreateRequest;
import com.example.ootd.domain.feed.dto.request.FeedCommentSearchCondition;
import com.example.ootd.domain.feed.dto.request.FeedCreateRequest;
import com.example.ootd.domain.feed.dto.request.FeedSearchCondition;
import com.example.ootd.domain.feed.dto.request.FeedUpdateRequest;
import com.example.ootd.dto.PageResponse;
import java.util.UUID;

/**
 * 피드 관리
 */
public interface FeedService {

  /**
   * 피드
   */
  // 피드 등록
  FeedDto createFeed(FeedCreateRequest request);

  // 피드 수정
  FeedDto updateFeed(UUID feedId, FeedUpdateRequest request, UUID userId);

  // 피드 목록 조회
  PageResponse<FeedDto> findFeedByCondition(FeedSearchCondition request, UUID userId);

  // 피드 삭제
  void deleteFeed(UUID feedId);

  /**
   * 피드 좋아요
   */
  // 피드 좋아요
  FeedDto likeFeed(UUID feedId, UUID userId);

  // 피드 좋아요 취소
  void deleteFeedLike(UUID feedId, UUID userId);

  /**
   * 피드 댓글
   */
  // 피드 댓글 등록
  CommentDto createComment(CommentCreateRequest request);

  // 피드 댓글 조회
  PageResponse<CommentDto> findCommentByCondition(UUID feedId,
      FeedCommentSearchCondition condition);
}
