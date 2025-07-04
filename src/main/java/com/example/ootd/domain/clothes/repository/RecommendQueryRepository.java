package com.example.ootd.domain.clothes.repository;

import com.example.ootd.domain.clothes.entity.Clothes;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RecommendQueryRepository extends JpaRepository<Clothes, UUID> {

  /**
   * 옷 추천 쿼리
   * 1. SELECT
   *  clothes -> id, name, type, image_url
   *  weather -> temperature_current, precipitation_amount, humidity_current, wind_speed
   *  user -> temperature_sensitivity
   *  clothes_attributes -> thickness
   * 2. CASE
   * 점수 계산
   * 3. FROM
   * clothes c
   * 4. JOIN
   * JOIN weather w
   * JOIN user u
   * LEFT JOIN clothes_attributes ca ON c.id = ca.clothes_id
   * LEFT JOIN attributes a ON ca.attribute_id = a.id AND a.name = '두께감'
   * 5. WHERE
   * c.user_id = :userId
   * 6. ORDER BY
   * score DESC
   *
   * etc.
   * 체감 온도 간단 버전 -> 실제 온도 - (풍속 * 0.8) + (습도 * 0.04)
   * WHEN 비가 오면 -2 보정
   *
   * 민감 온도 0 ~ 5
   * 체감 온도 + 민감 온도 = 절대 온도
   * 절대 온도 + 옷 두께에 따른 점수 계산
   * 기본 점수 60 + 알맞게 입었을 때 30점 else 10점 ?
   * 세세하게 CASE 나눠서 계산 할지 ?
   */
    @Query(value = """
        SELECT 
            c.id, 
            c.name, 
            c.type, 
            i.url as image_url,
            w.temperature_current, 
            w.precipitation_amount, 
            w.humidity_current, 
            w.wind_speed,
            u.temperature_sensitivity,
            ca.value as thickness,

            -- 점수 계산
            CASE
                --- 체감 온도 + 민감 온도 >= 30도
                WHEN ((w.temperature_current - (w.wind_speed * 0.8) + (w.humidity_current * 0.04) +
                CASE WHEN w.precipitation_amount > 0 THEN -2 ELSE 0 END) + u.temperature_sensitivity) >= 30 THEN 60 +
                 CASE
                     WHEN ca.value = '얇음' THEN 30
                     WHEN ca.value = '약간 얇음' THEN 15
                     ELSE 0
                 END
                 --- 체감 온도 + 민감 온도 >= 20도
                WHEN ((w.temperature_current - (w.wind_speed * 0.8) + (w.humidity_current * 0.04) +
                CASE WHEN w.precipitation_amount > 0 THEN -2 ELSE 0 END) + u.temperature_sensitivity) >= 20 THEN 60 +
                 CASE
                    WHEN ca.value = '얇음' THEN 25
                    WHEN ca.value = '약간 얇음' THEN 17
                    WHEN ca.value = '약간 두꺼움' THEN 3
                    ELSE 0
                END
                --- 체감 온도 + 민감 온도 >= 10도
                WHEN ((w.temperature_current - (w.wind_speed * 0.8) + (w.humidity_current * 0.04) +
                CASE WHEN w.precipitation_amount > 0 THEN -2 ELSE 0 END) + u.temperature_sensitivity) >= 10 THEN 60 +
                  CASE
                    WHEN ca.value = '약간 두꺼움' THEN 27
                    WHEN ca.value = '약간 얇음' THEN 19
                    WHEN ca.value = '두꺼움' THEN 16
                    ELSE 0
                  END
               --- 체감 온도 + 민감 온도 < 10도
               WHEN ((w.temperature_current - (w.wind_speed * 0.8) + (w.humidity_current * 0.04) +
               CASE WHEN w.precipitation_amount > 0 THEN -2 ELSE 0 END) + u.temperature_sensitivity) < 10 THEN 60 +
                 CASE
                    WHEN ca.value = '두꺼움' THEN 29
                    WHEN ca.value = '약간 두꺼움' THEN 18
                    WHEN ca.value = '약간 얇음' THEN 1
                    ELSE 0
                 END
               ELSE 60
        END AS score
            
        FROM clothes c
        LEFT JOIN images i ON c.image_id = i.id
        JOIN weather w ON w.id = :weatherId
        JOIN users u ON u.id = :userId
        LEFT JOIN clothes_attributes ca ON c.id = ca.clothes_id
        LEFT JOIN attributes a ON ca.attribute_id = a.id AND a.name = '두께감'
        WHERE c.user_id = :userId
        ORDER BY score DESC
        """, nativeQuery = true)
    List<Object[]> findClothesRecommendations(@Param("weatherId") UUID weatherId,
                                              @Param("userId") UUID userId);
}
