package com.innowise.authservice.service;

import com.innowise.authservice.entity.Credentials;
import org.springframework.security.oauth2.jwt.Jwt;

public interface TokenService {
  String generateAccessToken(Credentials credentials);
  String generateRefreshToken(Credentials credentials);
  String generateServiceToken();
  Jwt parse(String token, String expectedType);
}
