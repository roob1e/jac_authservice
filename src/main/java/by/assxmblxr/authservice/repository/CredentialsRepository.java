package by.assxmblxr.authservice.repository;

import by.assxmblxr.authservice.entity.Credentials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CredentialsRepository extends JpaRepository<Credentials, Long> {
  Optional<Credentials> findByLogin(String login);

  boolean existsByLogin(String login);
}
