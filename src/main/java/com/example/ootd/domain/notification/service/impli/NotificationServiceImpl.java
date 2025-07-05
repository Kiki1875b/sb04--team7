package com.example.ootd.domain.notification.service.impli;

import com.example.ootd.domain.notification.dto.NotificationDto;
import com.example.ootd.domain.notification.dto.NotificationRequest;
import com.example.ootd.domain.notification.entity.Notification;
import com.example.ootd.domain.notification.mapper.NotificationMapper;
import com.example.ootd.domain.notification.repository.NotificationRepository;
import com.example.ootd.domain.notification.service.inter.NotificationServiceInterface;
import com.example.ootd.dto.PageResponse;
import com.mongodb.bulk.BulkWriteResult;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationServiceInterface {

  private final NotificationRepository repository;
  private final MongoTemplate mongoTemplate;
  private final NotificationMapper notificationMapper;

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public NotificationDto createNotification(NotificationDto dto) {

    try {
      Notification notification = Notification.createNotification(dto);
      repository.save(notification);
      return notificationMapper.toDto(notification);
    } catch (Exception e) {
      log.warn("알림 오류", e);
      throw new IllegalArgumentException(
          "알림 오류", e);
    }
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public NotificationDto createNotification(NotificationRequest req) {
    try {
      Notification notification = Notification.createNotification(req);
      repository.save(notification);
      return notificationMapper.toDto(notification);
    } catch (Exception e) {
      log.warn("알림 오류", e);
      throw new IllegalArgumentException(
          "알림 오류", e);
    }

  }

  @Override
  @Transactional(readOnly = true)
  public NotificationDto get(UUID notificationId) {
    try {
      Notification notification = repository.findById(notificationId).orElseThrow(() ->
          new IllegalArgumentException("Notification 없음 " + notificationId));
      ;
      return notificationMapper.toDto(notification);
    } catch (IllegalArgumentException e) {
      log.warn("알림 오류", e);
      throw new IllegalArgumentException(
          "알림 오류", e);
    }
  }

  @Override
  @Transactional(readOnly = true) //페이지네이션
  public PageResponse getPageNation(UUID receiverId, String cursor, int limit) {

    try {
      List<Notification> slice;
      if (cursor == null || cursor.isBlank()) {
        slice = repository.findByReceiverIdOrderByCreatedAtDesc(
            receiverId,
            PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
      } else {
        Instant before = Instant.parse(cursor);
        slice = repository.findByReceiverIdAndCreatedAtBeforeOrderByCreatedAtDesc(
            receiverId,
            before,
            PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
      }

      List<NotificationDto> dtos = slice.stream()
          .map(notificationMapper::toDto)
          .toList();

      boolean hasNext = dtos.size() == limit;
      String nextCursor = null;
      UUID nextIdAfter = null;

      if (hasNext) {
        NotificationDto last = dtos.get(dtos.size() - 1);
        nextCursor = last.createdAt().toString();
        nextIdAfter = last.id();
      }
      int totalCount = repository.countByReceiverId(receiverId); //나중에 캐시 넣기
      return new PageResponse(
          dtos,
          hasNext,
          nextCursor,
          nextIdAfter,
          "createdAt",
          "DESC",
          totalCount
      );
    } catch (Exception e) {
      log.warn("알림 오류", e);
      throw new IllegalArgumentException(
          "알림 오류", e);
    }
  }

  @Override
  @Transactional
  public void readNotification(UUID notificationId) {
    try {
      Notification notification = repository.findById(notificationId).orElseThrow(() ->
          new IllegalArgumentException("Notification 없음 " + notificationId));
      repository.delete(notification);
    } catch (IllegalArgumentException e) {
      log.warn("알림 오류", e);
      throw new IllegalArgumentException(
          "알림 오류", e);
    }
  }

  @Transactional(propagation = Propagation.NOT_SUPPORTED)//벌크용 몽고db에 한번에 쓰기
  public List<NotificationDto> createAll(List<NotificationRequest> reqs) {

    BulkOperations ops =
        mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, Notification.class); // 병렬 작업

    reqs.forEach(r -> ops.insert(toDocument(r)));
    try {
      BulkWriteResult result = ops.execute(); //큐에있는 작업 실행
    } catch (RuntimeException e) {
      log.error("벌크 저장중 오류", e);
      throw e;
    }

    return reqs.stream()
        .map(this::toDocument)
        .map(notificationMapper::toDto)
        .toList();
  }

  private Notification toDocument(NotificationRequest req) {
    return Notification.createNotification(req);
  }

  private void executeBulkInsert(List<NotificationRequest> batch) {
    BulkOperations ops =
        mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, Notification.class);
    batch.forEach(r -> ops.insert(toDocument(r)));
    ops.execute();
  }
}
