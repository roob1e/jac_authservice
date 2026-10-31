package com.innowise.authservice.controller;

import com.innowise.authservice.dto.LoginRequest;
import com.innowise.authservice.dto.RefreshRequest;
import com.innowise.authservice.dto.RegisterRequest;
import com.innowise.authservice.dto.RegisterResponse;
import com.innowise.authservice.dto.TokenResponse;
import com.innowise.authservice.dto.ValidateRequest;
import com.innowise.authservice.dto.ValidateResponse;
import com.innowise.authservice.service.CredentialsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class CredentialsController {
  private final CredentialsService credentialsService;

  @PostMapping("/register")
  public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
    RegisterResponse response = credentialsService.register(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @PostMapping("/login")
  public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
    TokenResponse response = credentialsService.login(request);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/refresh")
  public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
    TokenResponse response = credentialsService.refresh(request);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/validate")
  public ResponseEntity<ValidateResponse> validate(@Valid @RequestBody ValidateRequest request) {
    ValidateResponse response = credentialsService.validate(request);
    return ResponseEntity.ok(response);
  }
}
