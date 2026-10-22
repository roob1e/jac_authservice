package by.assxmblxr.authservice.service.impl;

import by.assxmblxr.authservice.client.UserServiceClient;
import by.assxmblxr.authservice.dto.*;
import by.assxmblxr.authservice.entity.Credentials;
import by.assxmblxr.authservice.entity.Role;
import by.assxmblxr.authservice.exception.AccountDisabledException;
import by.assxmblxr.authservice.exception.InvalidCredentialsException;
import by.assxmblxr.authservice.exception.InvalidTokenException;
import by.assxmblxr.authservice.exception.LoginAlreadyExistsException;
import by.assxmblxr.authservice.repository.CredentialsRepository;
import by.assxmblxr.authservice.service.CredentialsService;
import by.assxmblxr.authservice.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CredentialsServiceImpl implements CredentialsService {
  private final CredentialsRepository credentialsRepository;
  private final UserServiceClient userServiceClient;
  private final PasswordEncoder passwordEncoder;
  private final TokenService tokenService;

  @Override
  public RegisterResponse register(RegisterRequest request) {
    if (credentialsRepository.existsByLogin(request.login())) {
      throw new LoginAlreadyExistsException("Login \"" + request.login() + "\" already exists");
    }
    Long userId = userServiceClient.createUser(request.firstName(), request.lastName(), request.email());
    String passwordHash = passwordEncoder.encode(request.password());
    Credentials credentials = Credentials.builder()
            .userId(userId)
            .login(request.login())
            .passwordHash(passwordHash)
            .role(Role.USER)
            .build();

    try {
      credentials = credentialsRepository.save(credentials);
    } catch (RuntimeException e) {
      try {
        userServiceClient.deleteUser(userId);
      } catch (RuntimeException deleteError) {
        e.addSuppressed(deleteError);
      }
      throw e;
    }
    return new RegisterResponse(
            credentials.getId(),
            userId,
            credentials.getLogin(),
            credentials.getRole()
    );
  }

  @Override
  public TokenResponse login(LoginRequest request) {
    Credentials credentials = credentialsRepository.findByLogin(request.login())
            .orElseThrow(() -> new InvalidCredentialsException("Invalid login or password"));
    if (!passwordEncoder.matches(request.password(), credentials.getPasswordHash())) {
      throw new InvalidCredentialsException("Invalid login or password");
    }
    if (!credentials.isActive()) {
      throw new AccountDisabledException("Account disabled");
    }
    String accessToken = tokenService.generateAccessToken(credentials);
    String refreshToken = tokenService.generateRefreshToken(credentials);
    return new TokenResponse(accessToken, refreshToken);
  }

  @Override
  public TokenResponse refresh(RefreshRequest request) {
    Jwt token = tokenService.parse(request.refreshToken(), "refresh");
    Credentials credentials = credentialsRepository.findByLogin(token.getSubject())
            .orElseThrow(() -> new InvalidTokenException("Invalid token"));

    if (!credentials.isActive()) {
      throw new AccountDisabledException("Account disabled");
    }

    String accessToken = tokenService.generateAccessToken(credentials);
    String refreshToken = tokenService.generateRefreshToken(credentials);
    return new TokenResponse(accessToken, refreshToken);
  }

  @Override
  public ValidateResponse validate(ValidateRequest request) {
    Jwt token = tokenService.parse(request.accessToken(), "access");
    Long userId = token.getClaim("userId");
    String login = token.getSubject();
    String role = token.getClaim("role");
    return new ValidateResponse(userId, login, role);
  }
}
