package com.example.ootd.domain.clothes.repository.custom.impl;

import com.example.ootd.domain.clothes.dto.request.ClothesSearchCondition;
import com.example.ootd.domain.clothes.entity.Clothes;
import com.example.ootd.domain.clothes.entity.ClothesType;
import com.example.ootd.domain.clothes.entity.QClothes;
import com.example.ootd.domain.clothes.repository.custom.CustomClothesRepository;
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
public class CustomClothesRepositoryImpl implements CustomClothesRepository {

  private final JPAQueryFactory jpaQueryFactory;
  private final QClothes qClothes = QClothes.clothes;

  @Override
  public List<Clothes> findByCondition(ClothesSearchCondition condition) {
    return jpaQueryFactory
        .select(qClothes).distinct()
        .from(qClothes)
        .where(
            getWhere(condition.typeEqual(), condition.ownerId()),
            cursorCondition(condition.cursor(), condition.idAfter())
        )
        .orderBy(
            qClothes.createdAt.desc(),  // 최신순으로 정렬
            qClothes.id.asc()
        )
        .limit(condition.limit() + 1)
        .fetch();
  }

  @Override
  public long countByCondition(ClothesType typeEqual, UUID ownerId) {

    Long count = jpaQueryFactory
        .select(qClothes.count())
        .from(qClothes)
        .where(getWhere(typeEqual, ownerId))
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
  // 동일한 타입, 동일한 사용자 아이디
  private BooleanExpression getWhere(ClothesType typeEqual, UUID ownerId) {

    if (typeEqual == null) {
      return null;
    }

    return qClothes.type.eq(typeEqual)
        .and(qClothes.user.id.eq(ownerId));
  }

  /**
   * 커서 페이지네이션
   */
  // 커서(cursor) 세팅
  private BooleanExpression cursorCondition(String cursor, UUID idAfter) {

    if (!StringUtils.hasText(cursor)) {
      return afterCondition(idAfter);
    }

    LocalDateTime cursorCreatedAt = LocalDateTime.parse(cursor);

    return qClothes.createdAt.lt(cursorCreatedAt)
        .or(qClothes.createdAt.eq(cursorCreatedAt)
            .and(afterCondition(idAfter)));
  }

  // 보조 커서(idAfter) 세팅
  private BooleanExpression afterCondition(UUID idAfter) {
    if (idAfter == null) {
      return null;
    }

    return qClothes.id.gt(idAfter);
  }
}