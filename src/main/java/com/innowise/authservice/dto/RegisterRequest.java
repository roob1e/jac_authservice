package com.innowise.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record RegisterRequest(
        @NotBlank
        @Size(min = 5, max = 100, message = "Username must be 5-100 characters long")
        @Pattern(regexp = "^\\w+$", message = "Username may contain only Latin letters, digits and \"_\"")
        String username,

        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @NotBlank
        @Size(min = 8, max = 64, message = "Password must be 8-64 characters long")
        @Pattern(
                regexp = "^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z])[!-~]+$",
                message = "Password must contain uppercase and lowercase Latin letters and digits"
        )
        String password,

        @NotBlank
        @Size(max = 100)
        String name,

        @NotBlank
        @Size(max = 100)
        String surname,

        @NotNull
        @Past
        LocalDate birthDate
) {
}
