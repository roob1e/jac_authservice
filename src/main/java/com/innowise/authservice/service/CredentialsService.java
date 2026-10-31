package com.innowise.authservice.service;

import com.innowise.authservice.dto.LoginRequest;
import com.innowise.authservice.dto.RefreshRequest;
import com.innowise.authservice.dto.RegisterRequest;
import com.innowise.authservice.dto.RegisterResponse;
import com.innowise.authservice.dto.TokenResponse;
import com.innowise.authservice.dto.ValidateRequest;
import com.innowise.authservice.dto.ValidateResponse;

public interface CredentialsService {
  RegisterResponse register(RegisterRequest request);
  TokenResponse login(LoginRequest request);
  TokenResponse refresh(RefreshRequest request);
  ValidateResponse validate(ValidateRequest request);
}
