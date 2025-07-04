package com.example.ootd.domain.feed.repository.custom.impl;

import com.example.ootd.domain.feed.dto.request.FeedSearchCondition;
import com.example.ootd.domain.feed.entity.Feed;
import com.example.ootd.domain.feed.entity.QFeed;
import com.example.ootd.domain.feed.repository.custom.CustomFeedRepository;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
@RequiredArgsConstructor
public class CustomFeedRepositoryImpl implements CustomFeedRepository {

  private final JPAQueryFactory jpaQueryFactory;
  private final QFeed qFeed = QFeed.feed;

  @Override
  public List<Feed> findByCondition(FeedSearchCondition condition) {

    return jpaQueryFactory
        .select(qFeed).distinct()
        .from(qFeed)
        .where(
            getWhere(condition),
            cursorCondition(condition)
        )
        .orderBy(
            getOrderBy(condition.sortBy(), condition.sortDirection()),
            qFeed.id.asc()
        )
        .limit(condition.limit() + 1)
        .fetch();
  }

  @Override
  public long countByCondition(FeedSearchCondition condition) {

    Long count = jpaQueryFactory
        .select(qFeed.count())
        .from(qFeed)
        .where(getWhere(condition))
        .fetchOne();

    if (count == null) {
      return 0;
    } else {
      return count;
    }
  }

  /**
   * where절
   */
  // 키워드, 하늘 상태, 강수량, 작성자 id로 검색
  private BooleanBuilder getWhere(FeedSearchCondition condition) {

    BooleanBuilder where = new BooleanBuilder();

    if (condition.keywordLike() != null) {
      where.and(qFeed.content.contains(condition.keywordLike()));
    }
    if (condition.skyStatusEqual() != null) {
      where.and(qFeed.weather.skyStatus.eq(condition.skyStatusEqual()));
    }
    if (condition.precipitationTypeEqual() != null) {
      where.and(
          qFeed.weather.precipitation.precipitationType.eq(condition.precipitationTypeEqual()));
    }
    if (condition.authorIdEqual() != null) {
      where.and(qFeed.user.id.eq(condition.authorIdEqual()));
    }

    return where;
  }

  /**
   * 커서 페이지네이션
   */
  // 커서(cursor) 세팅
  private BooleanExpression cursorCondition(FeedSearchCondition condition) {

    boolean isDesc = "DESCENDING".equalsIgnoreCase(condition.sortDirection());

    if (StringUtils.hasText(condition.cursor())) {
      switch (condition.sortBy()) {
        case "createdAt":
          LocalDateTime cursorCreatedAt = LocalDateTime.parse(condition.cursor());
          if (isDesc) {
            // 내림차순일 경우 cursor값이 작거나, 같되 idAfter가 커야 함
            return qFeed.createdAt.lt(cursorCreatedAt)
                .or(qFeed.createdAt.eq(cursorCreatedAt)
                    .and(afterCondition(condition.idAfter())));
          } else {
            // 오름차순일 경우 cursor값이 크거나, 같되 idAfter가 커야 함
            return qFeed.createdAt.gt(cursorCreatedAt)
                .or(qFeed.createdAt.eq(cursorCreatedAt)
                    .and(afterCondition(condition.idAfter())));
          }
        case "likeCount":
          long cursorLikeCount = Long.parseLong(condition.cursor());
          if (isDesc) {
            // 내림차순일 경우 cursor값이 작거나, 같되 idAfter가 커야 함
            return qFeed.likeCount.lt(cursorLikeCount)
                .or(qFeed.likeCount.eq(cursorLikeCount)
                    .and(afterCondition(condition.idAfter())));
          } else {
            // 오름차순일 경우 cursor값이 크거나, 같되 idAfter가 커야 함
            return qFeed.likeCount.gt(cursorLikeCount)
                .or(qFeed.likeCount.eq(cursorLikeCount)
                    .and(afterCondition(condition.idAfter())));
          }
      }
    }

    return afterCondition(condition.idAfter());
  }

  // 보조 커서(idAfter) 세팅
  private BooleanExpression afterCondition(UUID idAfter) {

    if (idAfter == null) {
      return null;
    }

    return qFeed.id.gt(idAfter);
  }

  /**
   * orderBy절
   */
  // 생성일 or 좋아요 수
  private OrderSpecifier<?> getOrderBy(String sortBy, String sortDirection) {

    boolean isDesc = "DESCENDING".equalsIgnoreCase(sortDirection);

    switch (sortBy) {
      case "createdAt":  // 생성일
        if (isDesc) {
          return qFeed.createdAt.desc();
        } else {
          return qFeed.createdAt.asc();
        }
      case "likeCount":
        if (isDesc) {
          return qFeed.likeCount.desc();
        } else {
          return qFeed.likeCount.asc();
        }
      default:
        return qFeed.createdAt.desc(); // 기본 정렬: 최신순
    }
  }
}
