package com.innowise.authservice.dto;

import com.innowise.authservice.entity.Role;

public record RegisterResponse(
        Long id,
        Long userId,
        String username,
        Role role
) {
}
