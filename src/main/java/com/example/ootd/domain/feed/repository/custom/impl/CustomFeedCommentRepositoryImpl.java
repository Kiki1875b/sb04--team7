package com.example.ootd.domain.feed.repository.custom.impl;

import com.example.ootd.domain.feed.dto.request.FeedCommentSearchCondition;
import com.example.ootd.domain.feed.entity.FeedComment;
import com.example.ootd.domain.feed.entity.QFeedComment;
import com.example.ootd.domain.feed.repository.custom.CustomFeedCommentRepository;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CustomFeedCommentRepositoryImpl implements CustomFeedCommentRepository {

  private final JPAQueryFactory jpaQueryFactory;
  private final QFeedComment qFeedComment = QFeedComment.feedComment;

  @Override
  public List<FeedComment> findByCondition(FeedCommentSearchCondition condition) {

    return jpaQueryFactory
        .select(qFeedComment).distinct()
        .from(qFeedComment)
        .where(cursorCondition(condition))
        .orderBy(
            qFeedComment.createdAt.asc(),
            qFeedComment.id.asc()
        )
        .limit(condition.limit() + 1)
        .fetch();
  }

  /**
   * where절
   */
  private BooleanExpression cursorCondition(FeedCommentSearchCondition condition) {

    if (condition.cursor() != null) {
      return qFeedComment.createdAt.gt(condition.cursor())
          .or(qFeedComment.createdAt.eq(condition.cursor())
              .and(afterCondition(condition.idAfter())));
    }

    return afterCondition(condition.idAfter());
  }

  private BooleanExpression afterCondition(UUID idAfter) {

    if (idAfter == null) {
      return null;
    }

    return qFeedComment.id.gt(idAfter);
  }
}
