package com.innowise.authservice.dto;

public record ValidateResponse(
        Long userId,
        String username,
        String role
) {
}
