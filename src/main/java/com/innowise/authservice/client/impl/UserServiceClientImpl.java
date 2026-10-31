package com.innowise.authservice.client.impl;

import com.innowise.authservice.client.dto.CreateUserRequest;
import com.innowise.authservice.client.dto.CreateUserResponse;
import com.innowise.authservice.client.UserServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class UserServiceClientImpl implements UserServiceClient {
  private final WebClient userServiceWebClient;

  @Override
  public Long createUser(String name, String surname, String email, LocalDate birthDate) {
    CreateUserResponse response = userServiceWebClient
            .post()
            .uri("/api/users")
            .bodyValue(new CreateUserRequest(name, surname, email, birthDate))
            .retrieve()
            .bodyToMono(CreateUserResponse.class)
            .block();
    if (response == null || response.id() == null) {
      throw new IllegalStateException("UserService returned no user id");
    }
    return response.id();
  }

  @Override
  public void deleteUser(Long id) {
    userServiceWebClient
            .delete()
            .uri("/api/users/{id}", id)
            .retrieve()
            .toBodilessEntity()
            .block();
  }
}
