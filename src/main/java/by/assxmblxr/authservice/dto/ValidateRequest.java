package by.assxmblxr.authservice.dto;

import jakarta.validation.constraints.NotBlank;

public record ValidateRequest(
        @NotBlank
        String accessToken
) {
}
