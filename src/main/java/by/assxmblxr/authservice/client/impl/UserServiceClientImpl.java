package by.assxmblxr.authservice.client.impl;

import by.assxmblxr.authservice.client.dto.CreateUserRequest;
import by.assxmblxr.authservice.client.dto.CreateUserResponse;
import by.assxmblxr.authservice.client.UserServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class UserServiceClientImpl implements UserServiceClient {
  private final WebClient userServiceWebClient;

  @Override
  public Long createUser(String firstName, String lastName, String email) {
    CreateUserResponse response = userServiceWebClient
            .post()
            .uri("/api/users")
            .bodyValue(new CreateUserRequest(firstName, lastName, email))
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
