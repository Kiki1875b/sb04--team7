package com.example.ootd.domain.clothes.controller;

import com.example.ootd.domain.clothes.dto.data.RecommendationDto;
import com.example.ootd.domain.clothes.service.RecommendService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/recommendations")
@Slf4j
public class RecommendController {

  private final RecommendService recommendService;

  @GetMapping
  public ResponseEntity<RecommendationDto> recommend(@RequestParam("weatherId") UUID weatherId) {
    RecommendationDto recommendation = recommendService.recommend(weatherId);
    log.debug("옷 추천 완료: {}", recommendation);
    return ResponseEntity.ok(recommendation);
  }

}
