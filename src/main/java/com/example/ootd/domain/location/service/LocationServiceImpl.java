package com.example.ootd.domain.location.service;

import com.example.ootd.domain.location.Location;
import com.example.ootd.domain.location.api.KakaoApiClient;
import com.example.ootd.domain.location.api.KakaoApiResponse;
import com.example.ootd.domain.location.dto.WeatherAPILocation;
import com.example.ootd.domain.location.repository.LocationRepository;
import com.example.ootd.domain.location.util.GridGpsConverter;
import com.example.ootd.domain.location.util.GridGpsConverter.LatXLngY;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class LocationServiceImpl implements LocationService {

  private final KakaoApiClient kakaoApiClient;
  private final LocationRepository locationRepository;

  @Override
  @Transactional(readOnly = true)
  public WeatherAPILocation getGridAndLocation(double latitude, double longitude) {

    // 위경도로 DB 조회 (좌표 반올림 기준도 고려 가능)
    Location location = locationRepository.findByLatitudeAndLongitude(latitude, longitude);
    if (location != null) {
      return new WeatherAPILocation(
          location.getLatitude(),
          location.getLongitude(),
          location.getLocationX(),
          location.getLocationY(),
          location.getLocationNames()
      );
    }

    // DB에 없을 경우 → Fallback to Kakao API
    List<KakaoApiResponse.Document> documents = kakaoApiClient.getAdministrativeRegions(longitude,
        latitude);
    LatXLngY grid = GridGpsConverter.convertGRID_GPS(GridGpsConverter.MODE_GRID, latitude,
        longitude);

    List<String> locationNames = documents.stream()
        .findFirst()
        .map(doc -> {
          List<String> names = List.of(
              doc.region_1depth_name(),
              doc.region_2depth_name(),
              doc.region_3depth_name(),
              doc.region_4depth_name()
          );
          log.debug("Kakao API에서 파싱된 지역명: {}", names);
          log.debug("1단계: '{}', 2단계: '{}', 3단계: '{}', 4단계: '{}'",
              doc.region_1depth_name(), doc.region_2depth_name(),
              doc.region_3depth_name(), doc.region_4depth_name());
          return names;
        })
        .orElse(List.of("알 수 없음"));

    // DB에 저장
    Location newLocation = new Location(latitude, longitude, grid.x.intValue(), grid.y.intValue(),
        locationNames);
    locationRepository.save(newLocation);

    return new WeatherAPILocation(
        newLocation.getLatitude(),
        newLocation.getLongitude(),
        newLocation.getLocationX(),
        newLocation.getLocationY(),
        newLocation.getLocationNames()
    );
  }
}