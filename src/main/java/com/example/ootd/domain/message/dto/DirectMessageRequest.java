package com.example.ootd.domain.message.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record DirectMessageRequest(
    @NotNull UUID receiverId, @NotNull UUID senderId, String content
) {

  @AssertTrue(message = "receiverId and senderId must differ")
  public boolean isReceiverDifferentFromSender() {
    return receiverId != null && senderId != null && !receiverId.equals(senderId);

  }
}
