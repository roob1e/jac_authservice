package by.assxmblxr.authservice.service;

import by.assxmblxr.authservice.dto.RegisterRequest;
import by.assxmblxr.authservice.dto.RegisterResponse;

public interface CredentialsService {
  RegisterResponse register(RegisterRequest request);
}
