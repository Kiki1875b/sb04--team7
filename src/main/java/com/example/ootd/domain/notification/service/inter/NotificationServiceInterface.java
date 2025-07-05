package com.example.ootd.domain.notification.service.inter;

import com.example.ootd.domain.notification.dto.NotificationDto;
import com.example.ootd.domain.notification.dto.NotificationRequest;
import com.example.ootd.dto.PageResponse;
import java.util.List;
import java.util.UUID;

public interface NotificationServiceInterface {

  NotificationDto createNotification(NotificationDto request);

  NotificationDto createNotification(NotificationRequest req);

  NotificationDto get(UUID NotificationId);//단일 조회, 쓸려나?

  PageResponse getPageNation(UUID receiverId, String cursor, int limit);//페이지네이션

  void readNotification(UUID NotificationId);//읽고나면 물리적 삭제, 논리적 삭제로 바꾸면 품이 꽤 듬

  List<NotificationDto> createAll(List<NotificationRequest> reqs);
}
