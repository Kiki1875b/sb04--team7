package com.example.ootd.domain.message.controller;

import com.example.ootd.domain.message.dto.DirectMessageDto;
import com.example.ootd.domain.message.dto.DirectMessageRequest;
import com.example.ootd.domain.message.dto.MessagePaginationDto;
import com.example.ootd.domain.message.dto.MessagePaginationRequest;
import com.example.ootd.domain.message.service.MessageServiceInterface;
import com.example.ootd.dto.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api/direct-messages")
public class MessageController {

  private final MessageServiceInterface messageService;

  @MessageMapping("/direct-messages_send")   // 클라이언트 가 /pub/direct-messages_send
  public void handleSend(@Valid @Payload DirectMessageRequest req) {
    messageService.sendMessage(req.senderId(), req.receiverId(), req.content());
  }

  @GetMapping
  public ResponseEntity<PageResponse> getMessage(
      @AuthenticationPrincipal Jwt jwt, //이거 보니 토큰에 있어서 따로 떼오는거로 나중에 변경
      @Valid @ModelAttribute MessagePaginationRequest request) {

    UUID ownerId = UUID.fromString(jwt.getClaimAsString("userId"));
    MessagePaginationDto messagePaginationDto = new MessagePaginationDto//아이디 넣어야해서 냅둠
        (ownerId, request.userId(), request.cursor(),
            request.isAfter(), request.limit());
    return ResponseEntity.ok(messageService.getMessage(messagePaginationDto));
  }


}
