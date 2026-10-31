package com.innowise.authservice.service;

import com.innowise.authservice.entity.Credentials;
import com.innowise.authservice.repository.CredentialsRepository;
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
