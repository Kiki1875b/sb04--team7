package com.example.ootd.domain.clothes.dto.data;

import com.example.ootd.domain.clothes.entity.ClothesType;
import java.util.UUID;
import lombok.Builder;

/**
 * 쿼리에서 점수가 계산된 의상 정보를 담는 DTO
 */
@Builder
public record ScoredClothesDto(
    UUID id,
    String name,
    ClothesType type,
    String imageUrl,
    Double temperatureCurrent,
    Double precipitationAmount,
    Double humidityCurrent,
    Double windSpeed,
    Integer temperatureSensitivity,
    String thickness,
    Integer score
) {
}
