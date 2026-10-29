package by.assxmblxr.authservice.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import by.assxmblxr.authservice.client.UserServiceClient;
import by.assxmblxr.authservice.dto.LoginRequest;
import by.assxmblxr.authservice.dto.RefreshRequest;
import by.assxmblxr.authservice.dto.RegisterRequest;
import by.assxmblxr.authservice.dto.RegisterResponse;
import by.assxmblxr.authservice.dto.TokenResponse;
import by.assxmblxr.authservice.dto.ValidateRequest;
import by.assxmblxr.authservice.dto.ValidateResponse;
import by.assxmblxr.authservice.entity.Credentials;
import by.assxmblxr.authservice.entity.Role;
import by.assxmblxr.authservice.exception.AccountDisabledException;
import by.assxmblxr.authservice.exception.InvalidCredentialsException;
import by.assxmblxr.authservice.exception.InvalidTokenException;
import by.assxmblxr.authservice.exception.UsernameAlreadyExistsException;
import by.assxmblxr.authservice.mapper.CredentialsMapper;
import by.assxmblxr.authservice.mapper.CredentialsMapperImpl;
import by.assxmblxr.authservice.repository.CredentialsRepository;
import by.assxmblxr.authservice.service.CredentialsPersistenceService;
import by.assxmblxr.authservice.service.TokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class CredentialsServiceImplTest {
  private static final Long USER_ID = 42L;
  private static final String USERNAME = "ivan_petrov";
  private static final String PASSWORD = "Secret123!";
  private static final String HASH = "$2a$10$hash";

  @Mock
  private CredentialsRepository credentialsRepository;

  @Mock
  private UserServiceClient userServiceClient;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private TokenService tokenService;

  @Mock
  private CredentialsPersistenceService credentialsPersistenceService;

  @Spy
  private CredentialsMapper credentialsMapper = new CredentialsMapperImpl();

  @InjectMocks
  private CredentialsServiceImpl credentialsService;

  @Test
  void registerCreatesUserProfileAndSavesHashedCredentialsWithUserRole() {
    when(credentialsRepository.existsByUsername(USERNAME)).thenReturn(false);
    when(userServiceClient.createUser("Ivan", "Petrov", "ivan@mail.com")).thenReturn(USER_ID);
    when(passwordEncoder.encode(PASSWORD)).thenReturn(HASH);
    when(credentialsPersistenceService.save(any())).thenAnswer(invocation -> {
      Credentials saved = invocation.getArgument(0);
      saved.setId(1L);
      return saved;
    });

    RegisterResponse response = credentialsService.register(registerRequest());

    ArgumentCaptor<Credentials> captor = ArgumentCaptor.forClass(Credentials.class);
    verify(credentialsPersistenceService).save(captor.capture());
    Credentials saved = captor.getValue();
    assertThat(saved.getUserId()).isEqualTo(USER_ID);
    assertThat(saved.getUsername()).isEqualTo(USERNAME);
    assertThat(saved.getPasswordHash()).isEqualTo(HASH);
    assertThat(saved.getRole()).isEqualTo(Role.USER);
    assertThat(saved.isActive()).isTrue();
    assertThat(response).isEqualTo(new RegisterResponse(1L, USER_ID, USERNAME, Role.USER));
    verify(userServiceClient, never()).deleteUser(anyLong());
  }

  @Test
  void registerThrowsWhenUsernameTakenWithoutCallingUserService() {
    when(credentialsRepository.existsByUsername(USERNAME)).thenReturn(true);

    assertThatThrownBy(() -> credentialsService.register(registerRequest()))
            .isInstanceOf(UsernameAlreadyExistsException.class);
    verify(userServiceClient, never()).createUser(anyString(), anyString(), anyString());
    verify(credentialsPersistenceService, never()).save(any());
  }

  @Test
  void registerDoesNotSaveCredentialsWhenUserServiceFails() {
    when(credentialsRepository.existsByUsername(USERNAME)).thenReturn(false);
    when(userServiceClient.createUser(anyString(), anyString(), anyString()))
            .thenThrow(new IllegalStateException("UserService returned no user id"));

    assertThatThrownBy(() -> credentialsService.register(registerRequest()))
            .isInstanceOf(IllegalStateException.class);
    verify(credentialsPersistenceService, never()).save(any());
    verify(userServiceClient, never()).deleteUser(anyLong());
  }

  @Test
  void registerDeletesCreatedUserProfileWhenSavingCredentialsFails() {
    DataIntegrityViolationException saveError = new DataIntegrityViolationException("duplicate user_id");
    when(credentialsRepository.existsByUsername(USERNAME)).thenReturn(false);
    when(userServiceClient.createUser(anyString(), anyString(), anyString())).thenReturn(USER_ID);
    when(passwordEncoder.encode(PASSWORD)).thenReturn(HASH);
    when(credentialsPersistenceService.save(any())).thenThrow(saveError);

    assertThatThrownBy(() -> credentialsService.register(registerRequest())).isSameAs(saveError);
    verify(userServiceClient).deleteUser(USER_ID);
  }

  @Test
  void registerKeepsOriginalErrorAndAttachesCompensationFailure() {
    DataIntegrityViolationException saveError = new DataIntegrityViolationException("duplicate user_id");
    IllegalStateException deleteError = new IllegalStateException("UserService is down");
    when(credentialsRepository.existsByUsername(USERNAME)).thenReturn(false);
    when(userServiceClient.createUser(anyString(), anyString(), anyString())).thenReturn(USER_ID);
    when(passwordEncoder.encode(PASSWORD)).thenReturn(HASH);
    when(credentialsPersistenceService.save(any())).thenThrow(saveError);
    doThrow(deleteError).when(userServiceClient).deleteUser(USER_ID);

    assertThatThrownBy(() -> credentialsService.register(registerRequest()))
            .isSameAs(saveError)
            .satisfies(e -> assertThat(e.getSuppressed()).containsExactly(deleteError));
  }

  @Test
  void loginReturnsAccessAndRefreshTokens() {
    Credentials credentials = credentials(true);
    when(credentialsRepository.findByUsername(USERNAME)).thenReturn(Optional.of(credentials));
    when(passwordEncoder.matches(PASSWORD, HASH)).thenReturn(true);
    when(tokenService.generateAccessToken(credentials)).thenReturn("access");
    when(tokenService.generateRefreshToken(credentials)).thenReturn("refresh");

    TokenResponse response = credentialsService.login(new LoginRequest(USERNAME, PASSWORD));

    assertThat(response).isEqualTo(new TokenResponse("access", "refresh"));
  }

  @Test
  void loginRejectsUnknownUsername() {
    when(credentialsRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> credentialsService.login(new LoginRequest(USERNAME, PASSWORD)))
            .isInstanceOf(InvalidCredentialsException.class)
            .hasMessage("Invalid username or password");
  }

  @Test
  void loginRejectsWrongPasswordWithSameMessageAsUnknownUsername() {
    when(credentialsRepository.findByUsername(USERNAME)).thenReturn(Optional.of(credentials(true)));
    when(passwordEncoder.matches("WrongPass1", HASH)).thenReturn(false);

    assertThatThrownBy(() -> credentialsService.login(new LoginRequest(USERNAME, "WrongPass1")))
            .isInstanceOf(InvalidCredentialsException.class)
            .hasMessage("Invalid username or password");
    verify(tokenService, never()).generateAccessToken(any());
  }

  @Test
  void loginRejectsDisabledAccountOnlyAfterPasswordCheck() {
    when(credentialsRepository.findByUsername(USERNAME)).thenReturn(Optional.of(credentials(false)));
    when(passwordEncoder.matches(PASSWORD, HASH)).thenReturn(true);

    assertThatThrownBy(() -> credentialsService.login(new LoginRequest(USERNAME, PASSWORD)))
            .isInstanceOf(AccountDisabledException.class);
    verify(tokenService, never()).generateAccessToken(any());
  }

  @Test
  void refreshIssuesNewTokensForActiveAccount() {
    Credentials credentials = credentials(true);
    when(tokenService.parse("refresh-token", "refresh")).thenReturn(jwt("refresh"));
    when(credentialsRepository.findByUsername(USERNAME)).thenReturn(Optional.of(credentials));
    when(tokenService.generateAccessToken(credentials)).thenReturn("new-access");
    when(tokenService.generateRefreshToken(credentials)).thenReturn("new-refresh");

    TokenResponse response = credentialsService.refresh(new RefreshRequest("refresh-token"));

    assertThat(response).isEqualTo(new TokenResponse("new-access", "new-refresh"));
  }

  @Test
  void refreshRejectsTokenOfDeletedAccount() {
    when(tokenService.parse("refresh-token", "refresh")).thenReturn(jwt("refresh"));
    when(credentialsRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> credentialsService.refresh(new RefreshRequest("refresh-token")))
            .isInstanceOf(InvalidTokenException.class);
  }

  @Test
  void refreshRejectsDisabledAccount() {
    when(tokenService.parse("refresh-token", "refresh")).thenReturn(jwt("refresh"));
    when(credentialsRepository.findByUsername(USERNAME)).thenReturn(Optional.of(credentials(false)));

    assertThatThrownBy(() -> credentialsService.refresh(new RefreshRequest("refresh-token")))
            .isInstanceOf(AccountDisabledException.class);
    verify(tokenService, never()).generateAccessToken(any());
  }

  @Test
  void refreshPropagatesInvalidTokenFromParser() {
    when(tokenService.parse("bad", "refresh")).thenThrow(new InvalidTokenException("Invalid token"));

    assertThatThrownBy(() -> credentialsService.refresh(new RefreshRequest("bad")))
            .isInstanceOf(InvalidTokenException.class);
    verify(credentialsRepository, never()).findByUsername(anyString());
  }

  @Test
  void validateReturnsTokenOwnerAndRole() {
    when(tokenService.parse("access-token", "access")).thenReturn(jwt("access"));

    ValidateResponse response = credentialsService.validate(new ValidateRequest("access-token"));

    assertThat(response).isEqualTo(new ValidateResponse(USER_ID, USERNAME, "USER"));
  }

  private RegisterRequest registerRequest() {
    return new RegisterRequest(USERNAME, "ivan@mail.com", PASSWORD, "Ivan", "Petrov");
  }

  private Credentials credentials(boolean active) {
    return Credentials.builder()
            .id(1L)
            .userId(USER_ID)
            .username(USERNAME)
            .passwordHash(HASH)
            .role(Role.USER)
            .active(active)
            .build();
  }

  private Jwt jwt(String type) {
    Instant now = Instant.now();
    return Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .subject(USERNAME)
            .claim("userId", USER_ID)
            .claim("role", "USER")
            .claim("type", type)
            .issuedAt(now)
            .expiresAt(now.plusSeconds(900))
            .build();
  }
}
