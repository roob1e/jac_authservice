package com.innowise.authservice.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.innowise.authservice.client.UserServiceClient;
import com.innowise.authservice.dto.LoginRequest;
import com.innowise.authservice.dto.RefreshRequest;
import com.innowise.authservice.dto.RegisterRequest;
import com.innowise.authservice.dto.RegisterResponse;
import com.innowise.authservice.dto.TokenResponse;
import com.innowise.authservice.dto.ValidateRequest;
import com.innowise.authservice.dto.ValidateResponse;
import com.innowise.authservice.entity.Credentials;
import com.innowise.authservice.entity.Role;
import com.innowise.authservice.exception.AccountDisabledException;
import com.innowise.authservice.exception.InvalidCredentialsException;
import com.innowise.authservice.exception.InvalidTokenException;
import com.innowise.authservice.exception.UsernameAlreadyExistsException;
import com.innowise.authservice.mapper.CredentialsMapper;
import com.innowise.authservice.mapper.CredentialsMapperImpl;
import com.innowise.authservice.repository.CredentialsRepository;
import com.innowise.authservice.service.CredentialsPersistenceService;
import com.innowise.authservice.service.TokenService;
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
import java.time.LocalDate;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class CredentialsServiceImplTest {
  private static final Long USER_ID = 42L;
  private static final String USERNAME = "ivan_petrov";
  private static final String PASSWORD = "Secret123!";
  private static final String HASH = "$2a$10$hash";
  private static final LocalDate BIRTH_DATE = LocalDate.of(1990, 5, 15);

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
    when(userServiceClient.createUser("Ivan", "Petrov", "ivan@mail.com", BIRTH_DATE)).thenReturn(USER_ID);
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
    verify(credentialsMapper).toEntity(registerRequest());
    verify(credentialsMapper).toRegisterResponse(saved);
    verify(userServiceClient, never()).deleteUser(anyLong());
  }

  @Test
  void registerThrowsWhenUsernameTakenWithoutCallingUserService() {
    when(credentialsRepository.existsByUsername(USERNAME)).thenReturn(true);

    RegisterRequest request = registerRequest();
    assertThatThrownBy(() -> credentialsService.register(request))
            .isInstanceOf(UsernameAlreadyExistsException.class);
    verify(userServiceClient, never()).createUser(anyString(), anyString(), anyString(), any(LocalDate.class));
    verify(credentialsPersistenceService, never()).save(any());
  }

  @Test
  void registerDoesNotSaveCredentialsWhenUserServiceFails() {
    when(credentialsRepository.existsByUsername(USERNAME)).thenReturn(false);
    when(userServiceClient.createUser(anyString(), anyString(), anyString(), any(LocalDate.class)))
            .thenThrow(new IllegalStateException("UserService returned no user id"));

    RegisterRequest request = registerRequest();
    assertThatThrownBy(() -> credentialsService.register(request))
            .isInstanceOf(IllegalStateException.class);
    verify(credentialsPersistenceService, never()).save(any());
    verify(userServiceClient, never()).deleteUser(anyLong());
  }

  @Test
  void registerDeletesCreatedUserProfileWhenSavingCredentialsFails() {
    DataIntegrityViolationException saveError = new DataIntegrityViolationException("duplicate user_id");
    when(credentialsRepository.existsByUsername(USERNAME)).thenReturn(false);
    when(userServiceClient.createUser(anyString(), anyString(), anyString(), any(LocalDate.class))).thenReturn(USER_ID);
    when(passwordEncoder.encode(PASSWORD)).thenReturn(HASH);
    when(credentialsPersistenceService.save(any())).thenThrow(saveError);

    RegisterRequest request = registerRequest();
    assertThatThrownBy(() -> credentialsService.register(request)).isSameAs(saveError);
    verify(userServiceClient).deleteUser(USER_ID);
  }

  @Test
  void registerKeepsOriginalErrorAndAttachesCompensationFailure() {
    DataIntegrityViolationException saveError = new DataIntegrityViolationException("duplicate user_id");
    IllegalStateException deleteError = new IllegalStateException("UserService is down");
    when(credentialsRepository.existsByUsername(USERNAME)).thenReturn(false);
    when(userServiceClient.createUser(anyString(), anyString(), anyString(), any(LocalDate.class))).thenReturn(USER_ID);
    when(passwordEncoder.encode(PASSWORD)).thenReturn(HASH);
    when(credentialsPersistenceService.save(any())).thenThrow(saveError);
    doThrow(deleteError).when(userServiceClient).deleteUser(USER_ID);

    RegisterRequest request = registerRequest();
    assertThatThrownBy(() -> credentialsService.register(request))
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

    LoginRequest request = new LoginRequest(USERNAME, PASSWORD);
    assertThatThrownBy(() -> credentialsService.login(request))
            .isInstanceOf(InvalidCredentialsException.class)
            .hasMessage("Invalid username or password");
  }

  @Test
  void loginRejectsWrongPasswordWithSameMessageAsUnknownUsername() {
    when(credentialsRepository.findByUsername(USERNAME)).thenReturn(Optional.of(credentials(true)));
    when(passwordEncoder.matches("WrongPass1", HASH)).thenReturn(false);

    LoginRequest request = new LoginRequest(USERNAME, "WrongPass1");
    assertThatThrownBy(() -> credentialsService.login(request))
            .isInstanceOf(InvalidCredentialsException.class)
            .hasMessage("Invalid username or password");
    verify(tokenService, never()).generateAccessToken(any());
  }

  @Test
  void loginRejectsDisabledAccountOnlyAfterPasswordCheck() {
    when(credentialsRepository.findByUsername(USERNAME)).thenReturn(Optional.of(credentials(false)));
    when(passwordEncoder.matches(PASSWORD, HASH)).thenReturn(true);

    LoginRequest request = new LoginRequest(USERNAME, PASSWORD);
    assertThatThrownBy(() -> credentialsService.login(request))
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

    RefreshRequest request = new RefreshRequest("refresh-token");
    assertThatThrownBy(() -> credentialsService.refresh(request))
            .isInstanceOf(InvalidTokenException.class);
  }

  @Test
  void refreshRejectsDisabledAccount() {
    when(tokenService.parse("refresh-token", "refresh")).thenReturn(jwt("refresh"));
    when(credentialsRepository.findByUsername(USERNAME)).thenReturn(Optional.of(credentials(false)));

    RefreshRequest request = new RefreshRequest("refresh-token");
    assertThatThrownBy(() -> credentialsService.refresh(request))
            .isInstanceOf(AccountDisabledException.class);
    verify(tokenService, never()).generateAccessToken(any());
  }

  @Test
  void refreshPropagatesInvalidTokenFromParser() {
    when(tokenService.parse("bad", "refresh")).thenThrow(new InvalidTokenException("Invalid token"));

    RefreshRequest request = new RefreshRequest("bad");
    assertThatThrownBy(() -> credentialsService.refresh(request))
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
    return new RegisterRequest(USERNAME, "ivan@mail.com", PASSWORD, "Ivan", "Petrov", BIRTH_DATE);
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
