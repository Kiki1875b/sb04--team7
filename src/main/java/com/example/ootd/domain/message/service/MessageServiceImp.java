package com.example.ootd.domain.message.service;

import com.example.ootd.domain.message.dto.DirectMessageDto;
import com.example.ootd.domain.message.dto.MessagePaginationDto;
import com.example.ootd.domain.message.entity.Message;
import com.example.ootd.domain.message.mapper.MessageMapper;
import com.example.ootd.domain.message.repository.MessageRepository;
import com.example.ootd.domain.user.User;
import com.example.ootd.domain.user.repository.UserRepository;
import com.example.ootd.dto.PageResponse;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Slf4j
public class MessageServiceImp implements MessageServiceInterface {

  private final MessageRepository messageRepository;
  private final SimpMessagingTemplate messagingTemplate;
  private final MessageMapper messageMapper;
  private final UserRepository userRepository;

  @Override
  @Transactional
  public DirectMessageDto sendMessage(UUID senderId, UUID receiverId, String content) {

    try {
      User sender = userRepository.findById(senderId)
          .orElseThrow(() -> new IllegalArgumentException("Sender 없음 : " + senderId));
      User receiver = userRepository.findById(receiverId)
          .orElseThrow(() -> new IllegalArgumentException("Receiver 없음 : " + receiverId));
      Message message = Message.createMessage(sender, receiver, content);
      messageRepository.save(message);
      DirectMessageDto dto = messageMapper.toDto(message);
      messagingTemplate.convertAndSend("/sub/direct-messages_" + message.getDmKey(), dto);
      return dto;
    } catch (IllegalArgumentException e) {
      log.error("메세지 전송중 오류");
      throw new RuntimeException(e);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponse getMessage(MessagePaginationDto req) {
    try {
      String dmKey = Message.makeDmKey(req.sender(), req.receiver());

      int limit = req.limit() > 0 ? req.limit() : 20;
      Sort.Direction dir = req.isAfter() ? Sort.Direction.ASC : Sort.Direction.DESC;

      Pageable pageable = PageRequest.of(0, limit + 1, Sort.by(dir, "id"));

      List<Message> messages;
      if (req.cursor() == null) {
        messages = messageRepository.findByDmKey(dmKey, pageable);
      } else if (req.isAfter()) {
        messages = messageRepository.findByDmKeyAndIdGreaterThan(dmKey, req.cursor(), pageable);
      } else {
        messages = messageRepository.findByDmKeyAndIdLessThan(dmKey, req.cursor(), pageable);
      }

      boolean hasNext = messages.size() > limit;
      if (hasNext) {
        messages = messages.subList(0, limit);
      }

      if (dir == Sort.Direction.DESC) {
        Collections.reverse(messages);
      }

      List<DirectMessageDto> data = messages.stream()
          .map(messageMapper::toDto)
          .collect(Collectors.toList());

      UUID nextCursor = hasNext && !messages.isEmpty()
          ? messages.get(messages.size() - 1).getId()
          : null;
      long totalCount = messageRepository.countByDmKey(dmKey);

      return new PageResponse(
          data, hasNext, nextCursor, null,
          "id", dir.name(), totalCount);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

  }
}
