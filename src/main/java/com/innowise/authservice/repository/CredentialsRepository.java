package com.innowise.authservice.repository;

import com.innowise.authservice.entity.Credentials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CredentialsRepository extends JpaRepository<Credentials, Long> {
  Optional<Credentials> findByUsername(String username);

  boolean existsByUsername(String username);
}
