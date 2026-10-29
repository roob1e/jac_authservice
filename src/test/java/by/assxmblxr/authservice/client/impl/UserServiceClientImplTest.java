package by.assxmblxr.authservice.client.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import by.assxmblxr.authservice.configuration.WebClientConfig;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

class UserServiceClientImplTest {
  private HttpServer server;
  private UserServiceClientImpl client;
  private final AtomicReference<String> lastRequest = new AtomicReference<>();

  @BeforeEach
  void setUp() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.start();
    client = clientFor("http://127.0.0.1:" + server.getAddress().getPort());
  }

  @AfterEach
  void tearDown() {
    server.stop(0);
  }

  @Test
  void createUserPostsProfileAndReturnsGeneratedId() {
    respond("/api/users", 201, "{\"id\": 42, \"email\": \"ivan@mail.com\", \"active\": true}");

    Long id = client.createUser("Ivan", "Petrov", "ivan@mail.com");

    assertThat(id).isEqualTo(42L);
    assertThat(lastRequest.get())
            .startsWith("POST /api/users ")
            .contains("\"firstName\":\"Ivan\"", "\"lastName\":\"Petrov\"", "\"email\":\"ivan@mail.com\"");
  }

  @Test
  void createUserFailsWhenResponseHasNoId() {
    respond("/api/users", 201, "{}");

    assertThatThrownBy(() -> client.createUser("Ivan", "Petrov", "ivan@mail.com"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("UserService returned no user id");
  }

  @Test
  void createUserPropagatesUserServiceClientError() {
    respond("/api/users", 409, "{\"status\": 409, \"message\": \"Email \\\"ivan@mail.com\\\" already exists\"}");

    assertThatThrownBy(() -> client.createUser("Ivan", "Petrov", "ivan@mail.com"))
            .isInstanceOfSatisfying(WebClientResponseException.class,
                    e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
  }

  @Test
  void deleteUserSendsDeleteForGivenId() {
    respond("/api/users/42", 204, null);

    client.deleteUser(42L);

    assertThat(lastRequest.get()).startsWith("DELETE /api/users/42 ");
  }

  @Test
  void unreachableUserServiceRaisesRequestException() {
    server.stop(0);

    assertThatThrownBy(() -> client.deleteUser(42L)).isInstanceOf(WebClientRequestException.class);
  }

  private UserServiceClientImpl clientFor(String baseUrl) {
    WebClient webClient = new WebClientConfig()
            .userServiceWebClient(WebClient.builder(), baseUrl, Duration.ofSeconds(2));
    return new UserServiceClientImpl(webClient);
  }

  private void respond(String path, int status, String json) {
    server.createContext(path, exchange -> {
      lastRequest.set(exchange.getRequestMethod() + " " + exchange.getRequestURI() + " " + body(exchange));
      if (json == null) {
        exchange.sendResponseHeaders(status, -1);
      } else {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
      }
      exchange.close();
    });
  }

  private String body(HttpExchange exchange) throws IOException {
    return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
  }
}
