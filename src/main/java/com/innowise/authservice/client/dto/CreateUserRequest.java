package com.innowise.authservice.client.dto;

import java.time.LocalDate;

public record CreateUserRequest(
        String name,
        String surname,
        String email,
        LocalDate birthDate
) {
}
