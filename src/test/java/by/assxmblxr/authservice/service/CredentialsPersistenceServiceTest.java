package by.assxmblxr.authservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import by.assxmblxr.authservice.entity.Credentials;
import by.assxmblxr.authservice.repository.CredentialsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CredentialsPersistenceServiceTest {
  @Mock
  private CredentialsRepository credentialsRepository;

  @InjectMocks
  private CredentialsPersistenceService credentialsPersistenceService;

  @Test
  void saveFlushesImmediatelySoConstraintViolationsSurfaceInsideTheCall() {
    Credentials credentials = Credentials.builder().username("ivan_petrov").build();
    when(credentialsRepository.saveAndFlush(credentials)).thenReturn(credentials);

    Credentials saved = credentialsPersistenceService.save(credentials);

    assertThat(saved).isSameAs(credentials);
    verify(credentialsRepository).saveAndFlush(credentials);
  }
}
