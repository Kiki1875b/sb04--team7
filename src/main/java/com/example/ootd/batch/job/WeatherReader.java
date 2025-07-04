package com.example.ootd.batch.job;

import com.example.ootd.batch.dto.RegionInfo;
import com.example.ootd.batch.dto.WeatherBatchData;
import com.example.ootd.domain.weather.api.WeatherApiClient;
import com.example.ootd.domain.weather.api.WeatherApiResponse;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class WeatherReader implements ItemReader<WeatherBatchData> {

  private final WeatherApiClient weatherApiClient;

  @Value("#{stepExecutionContext['regions']}")
  private List<RegionInfo> regions;

  @Value("#{stepExecutionContext['partitionId']}")
  private Integer partitionId;

  @Value("#{jobParameters['numOfRows'] ?: 1000}")
  private Integer numOfRows;

  @Value("${weather.api.delay.ms:100}")
  private long apiDelayMs;

  private Iterator<WeatherBatchData> weatherDataIterator;
  private boolean initialized = false;

  @Override
  public WeatherBatchData read() {
    if (!initialized) {
      initializeData();
      initialized = true;
    }

    if (weatherDataIterator != null && weatherDataIterator.hasNext()) {
      return weatherDataIterator.next();
    }
    return null;
  }

  private void initializeData() {
    log.info("Partition {} initializing with {} regions", partitionId, regions.size());
    List<WeatherBatchData> allWeatherData = collectWeatherData(regions, numOfRows);
    weatherDataIterator = allWeatherData.iterator();
    log.info("Partition {} initialized with {} weather data items", partitionId,
        allWeatherData.size());
  }

  private List<WeatherBatchData> collectWeatherData(List<RegionInfo> regions, int numOfRows) {
    List<WeatherBatchData> allWeatherData = new ArrayList<>();

    for (RegionInfo region : regions) {
      try {
        // 1차 API 호출: 현재 예보 데이터
        List<WeatherApiResponse.Item> currentItems = weatherApiClient.getVillageForecast(
            region.getNx(), region.getNy(), numOfRows
        );

        // 2차 API 호출: 전날 + TMN/TMX 데이터 (어제 02:00 기준 48시간)
        List<WeatherApiResponse.Item> previousItems = weatherApiClient.getTemperatureMinMax(
            region.getNx(), region.getNy()
        );

        // API 호출 제한을 위한 대기
        if (apiDelayMs > 0) {
          Thread.sleep(apiDelayMs);
        }

        // 현재 예보 데이터를 시간별로 그룹화
        Map<String, List<WeatherApiResponse.Item>> groupedByTime = currentItems.stream()
            .collect(Collectors.groupingBy(item ->
                item.fcstDate() + item.fcstTime()
            ));

        // 각 시간대별로 WeatherBatchData 생성
        groupedByTime.forEach((timeKey, timeItems) -> {
          WeatherBatchData weatherData = new WeatherBatchData();
          weatherData.setRegionName(region.getRegionName());
          weatherData.setNx(region.getNx());
          weatherData.setNy(region.getNy());
          weatherData.setItems(timeItems);
          // 전날 비교용 데이터와 TMN/TMX 데이터 추가
          weatherData.setPreviousItems(previousItems);
          allWeatherData.add(weatherData);
        });

      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        log.error("[Partition {}] Thread interrupted while fetching weather data", partitionId, e);
        break;
      } catch (Exception e) {
        log.error("[Partition {}] Failed to fetch weather data for region: {}",
            partitionId, region.getRegionName(), e);
      }
    }

    return allWeatherData;
  }
}