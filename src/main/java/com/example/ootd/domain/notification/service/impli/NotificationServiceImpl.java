package com.example.ootd.domain.notification.service.impli;

import com.example.ootd.domain.notification.dto.NotificationDto;
import com.example.ootd.domain.notification.dto.NotificationRequest;
import com.example.ootd.domain.notification.entity.Notification;
import com.example.ootd.domain.notification.mapper.NotificationMapper;
import com.example.ootd.domain.notification.repository.NotificationRepository;
import com.example.ootd.domain.notification.service.inter.NotificationServiceInterface;
import com.example.ootd.dto.PageResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationServiceInterface {

  private final NotificationRepository repository;
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
}
