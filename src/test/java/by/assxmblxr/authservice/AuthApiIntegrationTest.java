package by.assxmblxr.authservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import by.assxmblxr.authservice.client.UserServiceClient;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthApiIntegrationTest {
  private static final String PASSWORD = "Secret123!";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @MockitoBean
  private UserServiceClient userServiceClient;

  @BeforeEach
  void cleanDatabase() {
    jdbcTemplate.execute("TRUNCATE TABLE credentials RESTART IDENTITY");
  }

  @Test
  void registeredUserIsStoredWithHashedPasswordAndAuditTimestamps() throws Exception {
    when(userServiceClient.createUser("Ivan", "Petrov", "ivan@mail.com")).thenReturn(42L);

    mockMvc.perform(post("/api/auth/register")
                    .contentType(APPLICATION_JSON)
                    .content(registerJson("ivan_petrov", "ivan@mail.com")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.userId").value(42))
            .andExpect(jsonPath("$.username").value("ivan_petrov"))
            .andExpect(jsonPath("$.role").value("USER"))
            .andExpect(jsonPath("$.passwordHash").doesNotExist());

    Map<String, Object> row = jdbcTemplate.queryForMap("SELECT * FROM credentials WHERE username = 'ivan_petrov'");
    assertThat(row.get("user_id")).isEqualTo(42L);
    assertThat((String) row.get("password_hash")).startsWith("$2a$").isNotEqualTo(PASSWORD);
    assertThat(row.get("role")).isEqualTo("USER");
    assertThat(row.get("created_at")).isNotNull();
    assertThat(row.get("updated_at")).isNotNull();
  }

  @Test
  void samePasswordGetsDifferentHashesBecauseOfUniqueSalt() throws Exception {
    when(userServiceClient.createUser(anyString(), anyString(), anyString())).thenReturn(1L, 2L);
    register("ivan_petrov", "ivan@mail.com");
    register("anna_smirnova", "anna@mail.com");

    var hashes = jdbcTemplate.queryForList("SELECT password_hash FROM credentials", String.class);

    assertThat(hashes).hasSize(2).doesNotHaveDuplicates();
  }

  @Test
  void registerRejectsTakenUsernameWithoutCallingUserService() throws Exception {
    when(userServiceClient.createUser(anyString(), anyString(), anyString())).thenReturn(1L);
    register("ivan_petrov", "ivan@mail.com");

    mockMvc.perform(post("/api/auth/register")
                    .contentType(APPLICATION_JSON)
                    .content(registerJson("ivan_petrov", "other@mail.com")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409));

    verify(userServiceClient, never()).createUser("Ivan", "Petrov", "other@mail.com");
  }

  @Test
  void registerRollsBackUserProfileWhenCredentialsCannotBeSaved() throws Exception {
    when(userServiceClient.createUser(anyString(), anyString(), anyString())).thenReturn(7L);
    register("ivan_petrov", "ivan@mail.com");

    mockMvc.perform(post("/api/auth/register")
                    .contentType(APPLICATION_JSON)
                    .content(registerJson("anna_smirnova", "anna@mail.com")))
            .andExpect(status().isConflict());

    verify(userServiceClient).deleteUser(7L);
    assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM credentials", Long.class)).isEqualTo(1L);
  }

  @Test
  void registerRejectsInvalidFields() throws Exception {
    mockMvc.perform(post("/api/auth/register")
                    .contentType(APPLICATION_JSON)
                    .content("""
                            {"username": "iv", "password": "short", "email": "not-an-email",
                             "firstName": "", "lastName": "Petrov"}
                            """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(containsString("username")))
            .andExpect(jsonPath("$.message").value(containsString("password")))
            .andExpect(jsonPath("$.message").value(containsString("email")));

    verify(userServiceClient, never()).createUser(anyString(), anyString(), anyString());
  }

  @Test
  void loginRefreshAndValidateWorkTogether() throws Exception {
    when(userServiceClient.createUser(anyString(), anyString(), anyString())).thenReturn(42L);
    register("ivan_petrov", "ivan@mail.com");

    String tokens = mockMvc.perform(post("/api/auth/login")
                    .contentType(APPLICATION_JSON)
                    .content(loginJson("ivan_petrov", PASSWORD)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
    String accessToken = JsonPath.read(tokens, "$.accessToken");
    String refreshToken = JsonPath.read(tokens, "$.refreshToken");

    mockMvc.perform(post("/api/auth/validate")
                    .contentType(APPLICATION_JSON)
                    .content("{\"accessToken\": \"" + accessToken + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(42))
            .andExpect(jsonPath("$.username").value("ivan_petrov"))
            .andExpect(jsonPath("$.role").value("USER"));

    mockMvc.perform(post("/api/auth/refresh")
                    .contentType(APPLICATION_JSON)
                    .content("{\"refreshToken\": \"" + refreshToken + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty());

    mockMvc.perform(post("/api/auth/refresh")
                    .contentType(APPLICATION_JSON)
                    .content("{\"refreshToken\": \"" + accessToken + "\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Token type mismatch"));
  }

  @Test
  void loginRejectsWrongPasswordAndUnknownUserTheSameWay() throws Exception {
    when(userServiceClient.createUser(anyString(), anyString(), anyString())).thenReturn(42L);
    register("ivan_petrov", "ivan@mail.com");

    mockMvc.perform(post("/api/auth/login")
                    .contentType(APPLICATION_JSON)
                    .content(loginJson("ivan_petrov", "WrongPass1")))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Invalid username or password"));

    mockMvc.perform(post("/api/auth/login")
                    .contentType(APPLICATION_JSON)
                    .content(loginJson("nobody_here", PASSWORD)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Invalid username or password"));
  }

  @Test
  void disabledAccountCannotLogIn() throws Exception {
    when(userServiceClient.createUser(anyString(), anyString(), anyString())).thenReturn(42L);
    register("ivan_petrov", "ivan@mail.com");
    jdbcTemplate.update("UPDATE credentials SET active = false WHERE username = 'ivan_petrov'");

    mockMvc.perform(post("/api/auth/login")
                    .contentType(APPLICATION_JSON)
                    .content(loginJson("ivan_petrov", PASSWORD)))
            .andExpect(status().isForbidden());
  }

  @Test
  void validateRejectsGarbageToken() throws Exception {
    mockMvc.perform(post("/api/auth/validate")
                    .contentType(APPLICATION_JSON)
                    .content("{\"accessToken\": \"not.a.jwt\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.path").value("/api/auth/validate"));
  }

  @Test
  void jwksExposesOnlyPublicRsaKey() throws Exception {
    mockMvc.perform(get("/.well-known/jwks.json"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.keys[0].kty").value("RSA"))
            .andExpect(jsonPath("$.keys[0].alg").value("RS256"))
            .andExpect(jsonPath("$.keys[0].kid").isNotEmpty())
            .andExpect(jsonPath("$.keys[0].n").isNotEmpty())
            .andExpect(jsonPath("$.keys[0].d").doesNotExist())
            .andExpect(content().string(not(containsString("\"p\""))));
  }

  @Test
  void protectedPathWithoutTokenReturnsApiError401() throws Exception {
    mockMvc.perform(get("/api/something"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.path").value("/api/something"));
  }

  private void register(String username, String email) throws Exception {
    mockMvc.perform(post("/api/auth/register")
                    .contentType(APPLICATION_JSON)
                    .content(registerJson(username, email)))
            .andExpect(status().isCreated());
  }

  private String registerJson(String username, String email) {
    return """
            {"username": "%s", "password": "%s", "email": "%s", "firstName": "Ivan", "lastName": "Petrov"}
            """.formatted(username, PASSWORD, email);
  }

  private String loginJson(String username, String password) {
    return """
            {"username": "%s", "password": "%s"}
            """.formatted(username, password);
  }
}
