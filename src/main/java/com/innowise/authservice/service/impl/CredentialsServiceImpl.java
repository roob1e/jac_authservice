package com.innowise.authservice.service.impl;

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
import com.innowise.authservice.repository.CredentialsRepository;
import com.innowise.authservice.service.CredentialsPersistenceService;
import com.innowise.authservice.service.CredentialsService;
import com.innowise.authservice.service.TokenService;
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
  private final CredentialsPersistenceService credentialsPersistenceService;
  private final CredentialsMapper credentialsMapper;

  @Override
  public RegisterResponse register(RegisterRequest request) {
    if (credentialsRepository.existsByUsername(request.username())) {
      throw new UsernameAlreadyExistsException("Username \"" + request.username() + "\" already exists");
    }
    Long userId = userServiceClient.createUser(request.name(), request.surname(), request.email(), request.birthDate());
    Credentials credentials = credentialsMapper.toEntity(request);
    credentials.setUserId(userId);
    credentials.setPasswordHash(passwordEncoder.encode(request.password()));
    credentials.setRole(Role.USER);

    try {
      credentials = credentialsPersistenceService.save(credentials);
    } catch (RuntimeException e) {
      try {
        userServiceClient.deleteUser(userId);
      } catch (RuntimeException deleteError) {
        e.addSuppressed(deleteError);
      }
      throw e;
    }
    return credentialsMapper.toRegisterResponse(credentials);
  }

  @Override
  public TokenResponse login(LoginRequest request) {
    Credentials credentials = credentialsRepository.findByUsername(request.username())
            .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));
    if (!passwordEncoder.matches(request.password(), credentials.getPasswordHash())) {
      throw new InvalidCredentialsException("Invalid username or password");
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
    Credentials credentials = credentialsRepository.findByUsername(token.getSubject())
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
    String username = token.getSubject();
    String role = token.getClaim("role");
    return new ValidateResponse(userId, username, role);
  }
}
