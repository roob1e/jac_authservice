package by.assxmblxr.authservice.service;

import by.assxmblxr.authservice.dto.*;

public interface CredentialsService {
  RegisterResponse register(RegisterRequest request);
  TokenResponse login(LoginRequest request);
  TokenResponse refresh(RefreshRequest request);
  ValidateResponse validate(ValidateRequest request);
}
