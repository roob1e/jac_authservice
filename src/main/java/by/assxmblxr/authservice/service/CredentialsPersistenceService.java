package by.assxmblxr.authservice.service;

import by.assxmblxr.authservice.entity.Credentials;
import by.assxmblxr.authservice.repository.CredentialsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CredentialsPersistenceService {
  private final CredentialsRepository credentialsRepository;

  @Transactional
  public Credentials save(Credentials credentials) {
    return credentialsRepository.saveAndFlush(credentials);
  }
}
