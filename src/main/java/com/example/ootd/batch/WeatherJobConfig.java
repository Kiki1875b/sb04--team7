package com.example.ootd.batch;

import com.example.ootd.batch.dto.WeatherBatchData;
import com.example.ootd.batch.job.RegionPartitioner;
import com.example.ootd.batch.job.WeatherProcessor;
import com.example.ootd.batch.job.WeatherReader;
import com.example.ootd.batch.job.WeatherWriter;
import com.example.ootd.batch.listener.WeatherAlertListener;
import com.example.ootd.batch.listener.WeatherItemListener;
import com.example.ootd.batch.listener.WeatherJobExecutionListener;
import com.example.ootd.batch.listener.WeatherStepExecutionListener;
import com.example.ootd.domain.weather.entity.Weather;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class WeatherJobConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final RegionPartitioner regionPartitioner;
  private final WeatherReader weatherReader;
  private final WeatherProcessor weatherProcessor;
  private final WeatherWriter weatherWriter;
  private final WeatherItemListener weatherItemListener;
  private final WeatherAlertListener weatherAlertListener;
  private TaskExecutor weatherBatchTaskExecutor;

  @Autowired
  @Qualifier("weatherBatchTaskExecutor")
  public void setWeatherBatchTaskExecutor(TaskExecutor weatherBatchTaskExecutor) {
    this.weatherBatchTaskExecutor = weatherBatchTaskExecutor;
  }

  @Value("${weather.batch.chunk-size:10}")
  private int chunkSize;

  @Value("${weather.batch.grid-size:10}")
  private int gridSize;

  @Bean
  public Job weatherCollectJob() {
    return new JobBuilder("weatherCollectJob", jobRepository)
        .incrementer(new RunIdIncrementer())
        .start(weatherPartitionStep())
        .listener(jobExecutionListener())
        .build();
  }

  @Bean
  public Step weatherPartitionStep() {
    return new StepBuilder("weatherPartitionStep", jobRepository)
        .partitioner("weatherWorkerStep", regionPartitioner)
        .step(weatherWorkerStep())
        .gridSize(gridSize)
        .taskExecutor(weatherBatchTaskExecutor)
        .build();
  }

  @Bean
  public TaskletStep weatherWorkerStep() {
    return new StepBuilder("weatherWorkerStep", jobRepository)
        .<WeatherBatchData, Weather>chunk(chunkSize, transactionManager)
        .reader(weatherReader)
        .processor(weatherProcessor)
        .writer(weatherWriter)
        .faultTolerant()
        .retryLimit(1)
        .retry(Exception.class)
        .skipLimit(10)
        .skip(Exception.class)
        .listener(stepExecutionListener())
        .listener((StepExecutionListener) weatherItemListener)
        .listener((StepExecutionListener) weatherAlertListener)
        .build();
  }

  @Bean
  public WeatherJobExecutionListener jobExecutionListener() {
    return new WeatherJobExecutionListener();
  }

  @Bean
  public WeatherBatchNotificationService weatherBatchNotificationService() {
    return new WeatherBatchNotificationService();
  }

  @Bean
  public WeatherStepExecutionListener stepExecutionListener() {
    return new WeatherStepExecutionListener();
  }
}