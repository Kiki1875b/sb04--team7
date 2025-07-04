package com.example.ootd.batch.listener;

import com.example.ootd.batch.dto.WeatherBatchData;
import com.example.ootd.domain.notification.dto.NotificationEvent;
import com.example.ootd.domain.notification.enums.NotificationLevel;
import com.example.ootd.domain.notification.service.inter.NotificationPublisherInterface;
import com.example.ootd.domain.user.repository.UserRepository;
import com.example.ootd.domain.weather.entity.PrecipitationType;
import com.example.ootd.domain.weather.entity.Weather;
import com.example.ootd.domain.weather.entity.WindStrength;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.listener.StepListenerSupport;
import org.springframework.batch.item.Chunk;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WeatherAlertListener extends StepListenerSupport<WeatherBatchData, Weather> {

  @Getter
  private final ConcurrentHashMap<String, List<String>> failedRegions = new ConcurrentHashMap<>();

  // 중복 알림 방지를 위한 캐시 (지역_알림타입_날짜 -> 마지막 발송 시간)
  private final ConcurrentHashMap<String, Long> lastAlertTime = new ConcurrentHashMap<>();

  @Autowired
  private NotificationPublisherInterface notificationPublisher;

  @Autowired
  private UserRepository userRepository;

  // 날씨 알림 유형 정의
  public enum WeatherAlertType {
    HEAT_WAVE("폭염 경보"),
    HEAVY_RAIN("호우 경보"),
    SNOW_WARNING("대설 경보"),
    STRONG_WIND("강풍 경보");

    private final String message;

    WeatherAlertType(String message) {
      this.message = message;
    }

    public String getMessage() {
      return message;
    }
  }

  @Override
  public void afterRead(WeatherBatchData item) {
    log.debug("Successfully read data for region: {}", item.getRegionName());
  }

  @Override
  public void onReadError(Exception ex) {
    log.error("Error occurred while reading weather data", ex);
  }

  @Override
  public void afterProcess(WeatherBatchData item, Weather result) {
    if (result == null) {
      log.warn("Processing returned null for region: {}", item.getRegionName());
      return;
    }

//    안좋은 날씨인지 확인하기
//    log.debug(
//        " WeatherAlertListener DEBUG \nRegion: {}\nForecast Date: {}\nMax Temp: {}°C\nPrecipitation: {} ({}%)\nWind: {}",
//        result.getRegionName(),
//        result.getForecastAt().toLocalDate(),
//        result.getTemperature().getTemperatureMax(),
//        result.getPrecipitation().getPrecipitationType(),
//        result.getPrecipitation().getPrecipitationProbability(),
//        result.getWindSpeed().getWindAsWord());

    // 날씨 악화 감지 및 알림 발송
    try {
      detectAndSendWeatherAlerts(result);
    } catch (Exception e) {
      log.error("Failed to process weather alerts for region: {}", result.getRegionName(), e);
    }
  }

  @Override
  public void onProcessError(WeatherBatchData item, Exception e) {
    String regionName = item.getRegionName();
    log.error("Failed to process weather data for region: {}", regionName, e);
    failedRegions.computeIfAbsent("process", k -> new ArrayList<>()).add(regionName);
  }

  @Override
  public void onWriteError(Exception exception, Chunk<? extends Weather> items) {
    log.error("Failed to write weather data", exception);
    items.forEach(weather -> {
      failedRegions.computeIfAbsent("write", k -> new ArrayList<>())
          .add(weather.getRegionName());
    });
  }

  // 날씨 악화 감지 및 알림 발송
  private void detectAndSendWeatherAlerts(Weather weather) {
    // 날짜 필터링 먼저 체크 (당일 + 익일만)
    if (!shouldSendAlert(weather)) {
      return;
    }

    List<WeatherAlertType> alerts = detectWeatherAlerts(weather);

    if (!alerts.isEmpty()) {
      LocalDate forecastDate = weather.getForecastAt().toLocalDate();
      sendWeatherAlerts(weather.getRegionName(), alerts, forecastDate);
    }
  }

  // 알림 발송 여부 판단 (당일 + 익일만 알림)
  private boolean shouldSendAlert(Weather weather) {
    LocalDate today = LocalDate.now();
    LocalDate tomorrow = today.plusDays(1);
    LocalDate forecastDate = weather.getForecastAt().toLocalDate();

    // 오늘과 내일만 알림 발송
    boolean isWithinAlertPeriod = forecastDate.equals(today) || forecastDate.equals(tomorrow);

    if (!isWithinAlertPeriod) {
      log.debug("Skipping alert for future date: {} (forecast: {})",
          forecastDate, weather.getRegionName());
      return false;
    }

    return true;
  }

  // 날씨 악화 조건 검사
  private List<WeatherAlertType> detectWeatherAlerts(Weather weather) {
    List<WeatherAlertType> alerts = new ArrayList<>();

    // 폭염 감지 (최고기온 35도 이상, 낮 시간대)
    if (isHeatWave(weather)) {
      alerts.add(WeatherAlertType.HEAT_WAVE);
    }

    // 호우 감지 (강수확률 60% 이상 + 비)
    if (isHeavyRain(weather)) {
      alerts.add(WeatherAlertType.HEAVY_RAIN);
    }

    // 대설 감지 (강수확률 40% 이상 + 눈)
    if (isSnowWarning(weather)) {
      alerts.add(WeatherAlertType.SNOW_WARNING);
    }

    // 강풍 감지 (풍속 9m/s 이상)
    if (isStrongWind(weather)) {
      alerts.add(WeatherAlertType.STRONG_WIND);
    }

    return alerts;
  }

  // 폭염 조건 검사
  private boolean isHeatWave(Weather weather) {
    Double maxTemp = weather.getTemperature().getTemperatureMax();

    // 최고기온 35도 이상이면 시간 상관없이 폭염 위험
    return maxTemp != null && maxTemp >= 35.0;
  }

  // 호우 조건 검사
  private boolean isHeavyRain(Weather weather) {
    Double probability = weather.getPrecipitation().getPrecipitationProbability();
    PrecipitationType type = weather.getPrecipitation().getPrecipitationType();

    return type == PrecipitationType.RAIN &&
        probability != null &&
        probability >= 60.0;
  }

  // 대설 조건 검사
  private boolean isSnowWarning(Weather weather) {
    Double probability = weather.getPrecipitation().getPrecipitationProbability();
    PrecipitationType type = weather.getPrecipitation().getPrecipitationType();

    return (type == PrecipitationType.SNOW || type == PrecipitationType.RAIN_SNOW) &&
        probability != null &&
        probability >= 40.0;
  }

  // 강풍 조건 검사
  private boolean isStrongWind(Weather weather) {
    return weather.getWindSpeed().getWindAsWord() == WindStrength.STRONG;
  }

  // 날씨 알림 발송
  private void sendWeatherAlerts(String regionName, List<WeatherAlertType> alerts,
      LocalDate forecastDate) {
    try {
      // 중복 알림 방지 체크 (같은 날짜에 같은 경보는 한 번만)
      if (!shouldSendAnyAlert(regionName, alerts, forecastDate)) {
        log.debug("Skipping duplicate alerts for region: {} on date: {}", regionName, forecastDate);
        return;
      }

      // 알림 조건을 만족하는 경우에만 사용자 조회
      log.debug("Weather alert conditions met for {}: {}", regionName, alerts.stream()
          .map(WeatherAlertType::getMessage).toList());

      // 해당 지역 사용자들 조회
      List<UUID> regionUsers = findUsersByRegion(regionName);

      if (regionUsers.isEmpty()) {
        log.debug("No users found for region: {}", regionName);
        return;
      }

      // 알림 메시지 생성
      String title = regionName + " 날씨 경보";
      String content = buildAlertMessage(alerts, forecastDate);

      // 알림 발송
      NotificationEvent event = new NotificationEvent(
          title, content, NotificationLevel.WARNING
      );

      notificationPublisher.publishToMany(event, regionUsers);

      // 발송 시간 기록
      updateLastAlertTime(regionName, alerts, forecastDate);

      log.info("Weather alert sent to {} users in {} for {}: {}",
          regionUsers.size(), regionName, forecastDate, alerts.stream()
              .map(WeatherAlertType::getMessage).toList());

    } catch (Exception e) {
      log.error("Failed to send weather alert for region: {} on date: {}", regionName, forecastDate,
          e);
    }
  }

  // 중복 알림 방지 체크 (같은 날짜에 같은 경보는 한 번만)
  private boolean shouldSendAnyAlert(String regionName, List<WeatherAlertType> alerts,
      LocalDate forecastDate) {
    long now = System.currentTimeMillis();
    long oneDayAgo = now - TimeUnit.DAYS.toMillis(1);

    return alerts.stream().anyMatch(alert -> {
      String key = regionName + "_" + alert.name() + "_" + forecastDate;
      long lastTime = lastAlertTime.getOrDefault(key, 0L);
      return lastTime <= oneDayAgo;
    });
  }

  // 알림 발송 시간 기록 (날짜별로 기록)
  private void updateLastAlertTime(String regionName, List<WeatherAlertType> alerts,
      LocalDate forecastDate) {
    long now = System.currentTimeMillis();
    alerts.forEach(alert -> {
      String key = regionName + "_" + alert.name() + "_" + forecastDate;
      lastAlertTime.put(key, now);
    });
  }

  // 알림 메시지 생성
  private String buildAlertMessage(List<WeatherAlertType> alerts, LocalDate forecastDate) {
    StringBuilder message = new StringBuilder();

    // 날짜 정보 추가
    LocalDate today = LocalDate.now();
    String dateInfo = "";
    if (forecastDate.equals(today)) {
      dateInfo = "오늘";
    } else if (forecastDate.equals(today.plusDays(1))) {
      dateInfo = "내일";
    } else {
      dateInfo = forecastDate.toString();
    }

    message.append(dateInfo).append(" 날씨 상황:\n");

    for (WeatherAlertType alert : alerts) {
      message.append("⚠️ ")
          .append(alert.getMessage())
          .append("\n");
    }

    message.append("\n외출 시 주의하시기 바랍니다!");
    return message.toString();
  }

  // 지역별 사용자 조회
  private List<UUID> findUsersByRegion(String regionName) {
    try {
      // "서울특별시 마포구" -> ["서울특별시", "마포구"]
      String[] parts = regionName.split(" ");
      String city = parts[0];     // "서울특별시"
      String district = parts[1]; // "마포구" (무조건 있다고 가정)

      return userRepository.findUserIdsByRegion(city, district);

    } catch (Exception e) {
      log.error("Failed to find users for region: {}", regionName, e);
      return new ArrayList<>();
    }
  }

  public void clearFailedRegions() {
    failedRegions.clear();
  }

  // 정기적 캐시 정리 (매일 새벽 2시)
  @Scheduled(cron = "0 0 2 * * *")
  public void cleanupExpiredAlerts() {
    long now = System.currentTimeMillis();
    long threeDaysAgo = now - TimeUnit.DAYS.toMillis(3);

    int beforeSize = lastAlertTime.size();

    lastAlertTime.entrySet().removeIf(entry ->
        entry.getValue() < threeDaysAgo
    );

    int afterSize = lastAlertTime.size();
    log.info("Alert cache cleanup: {} -> {} entries (removed {} old entries)",
        beforeSize, afterSize, beforeSize - afterSize);
  }
}