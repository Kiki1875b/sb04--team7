package com.example.ootd.batch.listener;

import com.example.ootd.batch.dto.WeatherBatchData;
import com.example.ootd.domain.weather.entity.Weather;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.listener.StepListenerSupport;
import org.springframework.batch.item.Chunk;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WeatherItemListener extends StepListenerSupport<WeatherBatchData, Weather> {

  private final ConcurrentHashMap<String, List<String>> failedRegions = new ConcurrentHashMap<>();

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

  public ConcurrentHashMap<String, List<String>> getFailedRegions() {
    return failedRegions;
  }

  public void clearFailedRegions() {
    failedRegions.clear();
  }
}