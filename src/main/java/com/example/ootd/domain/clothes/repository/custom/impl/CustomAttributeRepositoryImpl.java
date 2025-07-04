package com.example.ootd.domain.clothes.repository.custom.impl;

import com.example.ootd.domain.clothes.dto.request.ClothesAttributeSearchCondition;
import com.example.ootd.domain.clothes.entity.Attribute;
import com.example.ootd.domain.clothes.entity.QAttribute;
import com.example.ootd.domain.clothes.repository.custom.CustomAttributeRepository;
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
public class CustomAttributeRepositoryImpl implements CustomAttributeRepository {

  private final JPAQueryFactory jpaQueryFactory;
  private final QAttribute qAttribute = QAttribute.attribute;

  @Override
  public List<Attribute> findByCondition(ClothesAttributeSearchCondition condition) {

    return jpaQueryFactory
        .select(qAttribute).distinct()
        .from(qAttribute)
        .where(
            getWhere(condition.keywordLike()),
            cursorCondition(condition)
        )
        .orderBy(
            getOrderBy(condition.sortBy(), condition.sortDirection()),
            qAttribute.id.asc()
        )
        .limit(condition.limit() + 1)
        .fetch();
  }

  @Override
  public long countByKeyword(String keywordLike) {

    Long count = jpaQueryFactory
        .select(qAttribute.count())
        .from(qAttribute)
        .where(getWhere(keywordLike))
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
  // 속성명, 속성 내용 검색
  private BooleanExpression getWhere(String keywordLike) {

    if (!StringUtils.hasText(keywordLike)) {
      return null;
    }

    return qAttribute.name.contains(keywordLike)
        .or(qAttribute.detailsRaw.contains(keywordLike));
  }

  /**
   * 커서 페이지네이션
   */
  // 커서(cursor) 세팅
  private BooleanExpression cursorCondition(ClothesAttributeSearchCondition condition) {

    boolean isDesc = "DESCENDING".equalsIgnoreCase(condition.sortDirection());

    if (StringUtils.hasText(condition.cursor())) {
      switch (condition.sortBy()) {
        case "name":
          String cursorName = condition.cursor();
          if (isDesc) { // 속성명
            return qAttribute.name.lt(cursorName);
          } else {
            return qAttribute.name.gt(cursorName);
          }
        case "createdAt": // 생성일
          LocalDateTime cursorCreatedAt = LocalDateTime.parse(condition.cursor());
          if (isDesc) {
            // 내림차순일 경우 cursor값이 작거나, 같되 idAfter가 커야 함
            return qAttribute.createdAt.lt(cursorCreatedAt)
                .or(qAttribute.createdAt.eq(cursorCreatedAt)
                    .and(afterCondition(condition.idAfter())));
          } else {
            // 오름차순일 경우 cursor값이 크거나, 같되 idAfter가 커야 함
            return qAttribute.createdAt.gt(cursorCreatedAt)
                .or(qAttribute.createdAt.eq(cursorCreatedAt)
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

    return qAttribute.id.gt(idAfter);
  }

  /**
   * orderBy절
   */
  // 속성명 or 생성일
  private OrderSpecifier<?> getOrderBy(String sortBy, String sortDirection) {

    boolean isDesc = "DESCENDING".equalsIgnoreCase(sortDirection);

    switch (sortBy) {
      case "name":  // 속성명
        if (isDesc) {
          return qAttribute.name.desc();
        } else {
          return qAttribute.name.asc();
        }
      case "createdAt":  // 생성일
        if (isDesc) {
          return qAttribute.createdAt.desc();
        } else {
          return qAttribute.createdAt.asc();
        }
      default:
        return qAttribute.name.asc(); // 기본 정렬: 속성명 오름차순
    }
  }
}
