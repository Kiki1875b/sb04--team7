package com.example.ootd.domain.message.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;
import org.springframework.lang.Nullable;

public record MessagePaginationRequest(
    @NotNull UUID userId,
    @Nullable UUID cursor,
    boolean isAfter,
    @Positive int limit
) {

}
