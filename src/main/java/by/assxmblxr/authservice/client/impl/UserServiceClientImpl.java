package by.assxmblxr.authservice.client.impl;

import by.assxmblxr.authservice.client.dto.CreateUserRequest;
import by.assxmblxr.authservice.client.dto.CreateUserResponse;
import by.assxmblxr.authservice.client.UserServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class UserServiceClientImpl implements UserServiceClient {
  private final RestClient restClient;

  @Override
  public Long createUser(String firstName, String lastName, String email) {
    CreateUserResponse response = restClient
            .post()
            .uri("/users")
            .body(new CreateUserRequest(firstName, lastName, email))
            .retrieve()
            .body(CreateUserResponse.class);
    if (response == null || response.id() == null) {
      throw new IllegalStateException("UserService returned no user id");
    }
    return response.id();
  }

  @Override
  public void deleteUser(Long id) {
    restClient.delete()
            .uri("/users/{id}", id)
            .retrieve()
            .toBodilessEntity();
  }
}
