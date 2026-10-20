package by.assxmblxr.authservice.service.impl;

import by.assxmblxr.authservice.client.UserServiceClient;
import by.assxmblxr.authservice.dto.RegisterRequest;
import by.assxmblxr.authservice.dto.RegisterResponse;
import by.assxmblxr.authservice.entity.Credentials;
import by.assxmblxr.authservice.entity.Role;
import by.assxmblxr.authservice.exception.LoginAlreadyExistsException;
import by.assxmblxr.authservice.repository.CredentialsRepository;
import by.assxmblxr.authservice.service.CredentialsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CredentialsServiceImpl implements CredentialsService {
  private final CredentialsRepository credentialsRepository;
  private final UserServiceClient userServiceClient;
  private final PasswordEncoder passwordEncoder;

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
}
