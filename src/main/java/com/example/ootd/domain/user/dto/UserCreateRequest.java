package com.example.ootd.domain.user.dto;

public record UserCreateRequest(
    String name,
    String email,
    String password
) {
}
