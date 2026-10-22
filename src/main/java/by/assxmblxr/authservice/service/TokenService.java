package by.assxmblxr.authservice.service;

import by.assxmblxr.authservice.entity.Credentials;
import org.springframework.security.oauth2.jwt.Jwt;

public interface TokenService {
  String generateAccessToken(Credentials credentials);
  String generateRefreshToken(Credentials credentials);
  Jwt parse(String token, String expectedType);
}
