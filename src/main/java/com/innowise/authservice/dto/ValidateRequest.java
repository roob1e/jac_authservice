package com.innowise.authservice.dto;

import jakarta.validation.constraints.NotBlank;

public record ValidateRequest(
        @NotBlank
        String accessToken
) {
}
