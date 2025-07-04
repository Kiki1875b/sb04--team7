package com.example.ootd.domain.clothes.service.impl;

import com.example.ootd.domain.clothes.dto.data.ClothesDto;
import com.example.ootd.domain.clothes.dto.data.ClothesAttributeWithDefDto;
import com.example.ootd.domain.clothes.dto.data.RecommendationDto;
import com.example.ootd.domain.clothes.dto.data.ScoredClothesDto;
import com.example.ootd.domain.clothes.entity.ClothesType;
import com.example.ootd.domain.clothes.repository.RecommendQueryRepository;
import com.example.ootd.domain.clothes.service.RecommendService;
import com.example.ootd.security.CustomUserDetails;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RecommendServiceImpl implements RecommendService {

  private final RecommendQueryRepository recommendQueryRepository;

  @Override
  @Transactional(readOnly = true)
  public RecommendationDto recommend(UUID weatherId) {
    log.info("옷 추천 요청: weatherId={}", weatherId);
    
    UUID userId = getCurrentUserId();

    // 쿼리 실행
    List<Object[]> queryResults = recommendQueryRepository
        .findClothesRecommendations(weatherId, userId);
    
    if (queryResults.isEmpty()) {
      log.debug("추천 가능한 의상이 없음: userId={}, weatherId={}", userId, weatherId);
      return RecommendationDto.builder()
          .weatherId(weatherId)
          .userId(userId)
          .clothes(List.of())
          .build();
    }
    
    // Object[] -> DTO 변환
    List<ScoredClothesDto> scoredClothes = queryResults.stream()
        .map(this::convertToScoredClothes)
        .toList();
    
    // 타입별 그룹
    Map<ClothesType, List<ScoredClothesDto>> clothesByType =
        scoredClothes.stream()
            .collect(Collectors.groupingBy(ScoredClothesDto::type));
    
    // 각 타입별 최고 점수 의상 선택
    List<ClothesDto> recommendedClothes = new ArrayList<>();
    
    // 필수 아이템
    addBestClothes(recommendedClothes, clothesByType, ClothesType.TOP);
    addBestClothes(recommendedClothes, clothesByType, ClothesType.BOTTOM);
    addBestClothes(recommendedClothes, clothesByType, ClothesType.SHOES);

    // 조건부 아이템 (체감온도 20도 미만일 때 아우터, 20도 이상이면 드레스)
    if (isWearingOuter(scoredClothes.get(0))) {
      addBestClothes(recommendedClothes, clothesByType, ClothesType.OUTER);
    }
    if (isWearingDress(scoredClothes.get(0))) {
      addBestClothes(recommendedClothes, clothesByType, ClothesType.DRESS);
    }

    
    // 나머지는 랜덤? TODO: 어떻게 조정할지 생각중 일단 50% 확률로 추가
    if (Math.random() < 0.5) {
      addBestClothes(recommendedClothes, clothesByType, ClothesType.ACCESSORY);
      addBestClothes(recommendedClothes, clothesByType, ClothesType.BAG);
      addBestClothes(recommendedClothes, clothesByType, ClothesType.HAT);
      addBestClothes(recommendedClothes, clothesByType, ClothesType.SCARF);
      addBestClothes(recommendedClothes, clothesByType, ClothesType.SOCKS);
      addBestClothes(recommendedClothes, clothesByType, ClothesType.UNDERWEAR);
      addBestClothes(recommendedClothes, clothesByType, ClothesType.ETC);
    }
    
    log.debug("추천 완료: {}개 의상 선택됨", recommendedClothes.size());
    
    return RecommendationDto.builder()
        .weatherId(weatherId)
        .userId(userId)
        .clothes(recommendedClothes)
        .build();
  }

  /**
   * Object[] 쿼리 결과를 ScoredClothesDto로 변환
   */
  private ScoredClothesDto convertToScoredClothes(Object[] row) {
    return ScoredClothesDto.builder()
        .id(UUID.fromString(row[0].toString()))
        .name(row[1].toString())
        .type(ClothesType.valueOf(row[2].toString()))
        .imageUrl(row[3] != null ? row[3].toString() : null)
        .temperatureCurrent(((Number) row[4]).doubleValue())
        .precipitationAmount(((Number) row[5]).doubleValue())
        .humidityCurrent(((Number) row[6]).doubleValue())
        .windSpeed(((Number) row[7]).doubleValue())
        .temperatureSensitivity(((Number) row[8]).intValue())
        .thickness(row[9] != null ? row[9].toString() : null)
        .score(((Number) row[10]).intValue())
        .build();
  }
  
  /**
   * 특정 타입의 최고 점수 의상을 추천 목록에 추가
   */
  private void addBestClothes(List<ClothesDto> recommendedClothes,
                            Map<ClothesType, List<ScoredClothesDto>> clothesByType,
                            ClothesType type) {
    clothesByType.getOrDefault(type, List.of())
        .stream()
        .findFirst()
        .ifPresent(clothes -> recommendedClothes.add(convertToClothesDto(clothes)));
  }
  
  /**
   * ScoredClothesDto를 ClothesDto로 변환
   */
  private ClothesDto convertToClothesDto(ScoredClothesDto scoredClothes) {
    return ClothesDto.builder()
        .id(scoredClothes.id())
        .name(scoredClothes.name())
        .type(scoredClothes.type())
        .imageUrl(scoredClothes.imageUrl())
        .attributes(createThicknessAttribute(scoredClothes.thickness()))
        .build();
  }
  
  /**
   * 아우터 착용 여부 판단 20(체감온도 기준)도 미만이면 아우터 착용
   */
  private boolean isWearingOuter(ScoredClothesDto weatherInfo) {
    double feltTemperature = weatherInfo.temperatureCurrent() 
        - (weatherInfo.windSpeed() * 0.8)
        + (weatherInfo.humidityCurrent() * 0.04)
        + (weatherInfo.precipitationAmount() > 0 ? -2 : 0)
        + weatherInfo.temperatureSensitivity();
    
    return feltTemperature < 20.0;
  }

  /**
   * 드레스 착용 여부 판단 20(체감온도 기준)도 이상이면 드레스 착용
   */
  private boolean isWearingDress(ScoredClothesDto weatherInfo) {
    double feltTemperature = weatherInfo.temperatureCurrent()
        - (weatherInfo.windSpeed() * 0.8)
        + (weatherInfo.humidityCurrent() * 0.04)
        + (weatherInfo.precipitationAmount() > 0 ? -2 : 0)
        + weatherInfo.temperatureSensitivity();

    return feltTemperature >= 20.0;
  }

  /**
   * 두께감 속성 생성
   */
  private List<ClothesAttributeWithDefDto> createThicknessAttribute(String thickness) {
    if (thickness == null || thickness.trim().isEmpty()) {
      return List.of();
    }
    
    return List.of(
        ClothesAttributeWithDefDto.builder()
            .definitionName("두께감")
            .value(thickness)
            .build()
    );
  }
  
  /**
   * 현재 인증된 사용자 ID 조회
   */
  private UUID getCurrentUserId() {
    CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext()
        .getAuthentication().getPrincipal();
    return userDetails.getUser().getId();
  }
}
