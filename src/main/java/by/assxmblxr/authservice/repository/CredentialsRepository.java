package by.assxmblxr.authservice.repository;

import by.assxmblxr.authservice.entity.Credentials;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CredentialsRepository extends JpaRepository<Credentials, Long> {
  Optional<Credentials> findByLogin(String login);
}
