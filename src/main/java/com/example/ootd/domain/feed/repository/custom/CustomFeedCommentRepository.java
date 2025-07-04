package com.example.ootd.domain.feed.repository.custom;

import com.example.ootd.domain.feed.dto.request.FeedCommentSearchCondition;
import com.example.ootd.domain.feed.entity.FeedComment;
import java.util.List;

public interface CustomFeedCommentRepository {

  List<FeedComment> findByCondition(FeedCommentSearchCondition condition);
}
