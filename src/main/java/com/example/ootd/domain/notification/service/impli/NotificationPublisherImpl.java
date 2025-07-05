package com.example.ootd.domain.notification.service.impli;

import static org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED;

import com.example.ootd.domain.notification.dto.NotificationBulk;
import com.example.ootd.domain.notification.dto.NotificationDto;
import com.example.ootd.domain.notification.dto.NotificationEvent;
import com.example.ootd.domain.notification.dto.NotificationRequest;
import com.example.ootd.domain.notification.service.inter.NotificationPublisherInterface;
import com.example.ootd.domain.notification.service.inter.NotificationServiceInterface;
import com.example.ootd.domain.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@RequiredArgsConstructor
@Service
@Slf4j
public class NotificationPublisherImpl implements NotificationPublisherInterface {

  //원래 카프카 설정
  //private final KafkaTemplate<String, NotificationDto> kafka;
//  private final EmitterRepository emitters;
//
//  @Value("${spring.kafka.template.default-topic:notification-events}")
//  private String topic;

  private final ApplicationEventPublisher eventPublisher;
  private final NotificationServiceInterface notificationService;
  private final UserRepository userRepository;
  private final int PAGE_SIZE = 600; //bulk안에 들어갈 수
  private final int BATCH_SIZE = 600;


  @Override
  @Async("notificationSingleExecutor")
  @Retryable(
      retryFor = Exception.class,          // 일단 모든 예외
      maxAttempts = 3,                      // 실패시 일단은 두번 추가 시도, 1,2,4초 텀 두고
      recover = "recoverNotification",
      backoff = @Backoff(delay = 1_000,
          multiplier = 2.0,
          maxDelay = 10_000)
  )
  @Transactional //이벤트 발행, 다른 서비스에서 이벤트 발생시킬떄 이거쓰면 됨
  public void publish(NotificationRequest req) {
    NotificationDto dto = notificationService.createNotification(req);
    eventPublisher.publishEvent(dto);
  }

  @Override
  @Async("notificationBulkExecutor")
//  @Retryable(
//      retryFor = Exception.class,          // 일단 모든 예외
//      maxAttempts = 3,                      // 실패시 일단은 두번 추가 시도, 1,2,4초 텀 두고
//      recover = "recoverNotification",
//      backoff = @Backoff(delay = 1_000,
//          multiplier = 2.0,
//          maxDelay = 10_000)
//  )
  @Transactional(propagation = NOT_SUPPORTED)//속도를 위해 선언 :: 이미 트랜잭션 있으면 없이 다시 실행
  public void publishToMany(NotificationEvent event, List<UUID> userIdList) {

    List<NotificationRequest> batch = new ArrayList<>(BATCH_SIZE);
    for (UUID id : userIdList) {
      batch.add(new NotificationRequest(id, event.title(), event.content(), event.level()));
      if (batch.size() == BATCH_SIZE) {
        List<NotificationDto> list = notificationService.createAll(batch);//한번에 생성
        eventPublisher.publishEvent(new NotificationBulk(List.copyOf(list)));
        batch.clear();
      }
    }
    if (!batch.isEmpty()) {// 안차고 남은거 이벤트 발행
      List<NotificationDto> list = notificationService.createAll(batch);
      eventPublisher.publishEvent(new NotificationBulk(List.copyOf(list)));
    }
  }

  @Override
  @Async("notificationBulkExecutor")
//  @Retryable(
//      retryFor = Exception.class,          // 일단 모든 예외
//      maxAttempts = 3,                      // 실패시 일단은 두번 추가 시도, 1,2,4초 텀 두고
//      recover = "recoverNotification",
//      backoff = @Backoff(delay = 1_000,
//          multiplier = 2.0,
//          maxDelay = 10_000)
//  )
  @Transactional(propagation = NOT_SUPPORTED)
  public void publishToAll(NotificationEvent event) {

    UUID cursor = null;
    boolean hasMore = true;
    while (hasMore) {
      Pageable pageable = PageRequest.of(0, PAGE_SIZE, Sort.by(Sort.Direction.ASC, "id"));
      List<UUID> userIds = userRepository.findIdsAfter(cursor, pageable);   // DB OOM 방지용
      List<NotificationRequest> batch = new ArrayList<>(BATCH_SIZE);
      for (UUID id : userIds) {
        batch.add(new NotificationRequest(id, event.title(), event.content(), event.level()));
        if (batch.size() == BATCH_SIZE) {
          List<NotificationDto> list = notificationService.createAll(batch); // bulk 삽입
          eventPublisher.publishEvent(new NotificationBulk(List.copyOf(list)));
          batch.clear();
        }
      }
      // 남은 알림 전송
      if (!batch.isEmpty()) {
        List<NotificationDto> list = notificationService.createAll(batch);
        eventPublisher.publishEvent(new NotificationBulk(List.copyOf(list)));
      }
      if (userIds.isEmpty()) {
        hasMore = false;
      } else {
        cursor = userIds.get(userIds.size() - 1);
        hasMore = userIds.size() == PAGE_SIZE;
      }
    }
  }

  @Recover
  public void recoverNotification(Exception ex, NotificationRequest req) {
    log.error("단일 알림 만들기에러 발생", ex, req);
  }

  @Recover
  public void recoverNotification(Exception ex, NotificationEvent req, List<UUID> userIdList) {
    log.error("다수 알림 만들기에러 발생", ex, req);
  }

  @Recover
  public void recoverNotification(Exception ex, NotificationEvent req) {
    log.error("모든 알림 만들기에러 발생", ex, req);
  }

}
