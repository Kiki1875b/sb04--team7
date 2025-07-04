package com.example.ootd.domain.location.api;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoApiClient {

  private final RestClient.Builder restClientBuilder;

  @Value("${kakao.api.key}")
  private String kakaoApiKey;

  private static final String BASE_URL = "https://dapi.kakao.com";

  public List<KakaoApiResponse.Document> getAdministrativeRegions(double longitude,
      double latitude) {

    if (longitude < -180 || longitude > 180 || latitude < -90 || latitude > 90) {
      throw new IllegalArgumentException(
          String.format("잘못된 좌표값입니다. longitude: %f, latitude: %f", longitude, latitude));
    }

    // 한국 내 좌표 범위 참고 검사
    if (latitude < 33.0 || latitude > 38.5 || longitude < 124.0 || longitude > 132.0) {
      log.warn("한국 외 지역 좌표일 가능성 - longitude: {}, latitude: {}", longitude, latitude);
    }

    String uri = UriComponentsBuilder
        .fromPath("/v2/local/geo/coord2regioncode.json")
        .queryParam("x", longitude)
        .queryParam("y", latitude)
        .build()
        .toUriString();

    log.debug("Kakao API 요청 URL: {}{}", BASE_URL, uri);

    try {
      KakaoApiResponse response = restClientBuilder
          .baseUrl(BASE_URL)
          .defaultHeader("Authorization", "KakaoAK " + kakaoApiKey)
          .build()
          .get()
          .uri(uri)
          .retrieve()
          .body(KakaoApiResponse.class);

      if (response == null || response.documents() == null || response.documents().isEmpty()) {
        log.warn("Kakao API에서 빈 응답 - longitude: {}, latitude: {}", longitude, latitude);
        throw new IllegalArgumentException("해당 좌표에대한 지역 정보를 찾을 수 없습니다.");
      }

      List<KakaoApiResponse.Document> result = response.documents().stream()
          .filter(doc -> "H".equals(doc.region_type()))
          .toList();

      return result;

    } catch (Exception e) {
      log.error("Kakao API 호출 실패 - longitude: {}, latitude: {}, error: {}",
          longitude, latitude, e.getMessage(), e);
      throw e;
    }
  }
}
