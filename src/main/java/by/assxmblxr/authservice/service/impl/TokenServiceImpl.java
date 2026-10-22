package by.assxmblxr.authservice.service.impl;

import by.assxmblxr.authservice.entity.Credentials;
import by.assxmblxr.authservice.exception.InvalidTokenException;
import by.assxmblxr.authservice.service.TokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class TokenServiceImpl implements TokenService {
  private final Duration accessTtl;
  private final Duration refreshTtl;
  private final JwtEncoder jwtEncoder;
  private final JwtDecoder jwtDecoder;

  public TokenServiceImpl(
          @Value("${jwt.access-ttl}") Duration accessTtl,
          @Value("${jwt.refresh-ttl}") Duration refreshTtl,
          JwtEncoder jwtEncoder, JwtDecoder jwtDecoder) {
    this.accessTtl = accessTtl;
    this.refreshTtl = refreshTtl;
    this.jwtEncoder = jwtEncoder;
    this.jwtDecoder = jwtDecoder;
  }

  @Override
  public String generateAccessToken(Credentials credentials) {
    Instant now = Instant.now();
    JwtClaimsSet claims = JwtClaimsSet.builder()
            .subject(credentials.getLogin())
            .issuedAt(now)
            .expiresAt(now.plus(accessTtl))
            .claim("userId", credentials.getUserId())
            .claim("role", credentials.getRole().name())
            .claim("type", "access")
            .build();

    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

    return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
  }

  @Override
  public String generateRefreshToken(Credentials credentials) {
    Instant now = Instant.now();
    JwtClaimsSet claims = JwtClaimsSet.builder()
            .subject(credentials.getLogin())
            .issuedAt(now)
            .expiresAt(now.plus(refreshTtl))
            .claim("userId", credentials.getUserId())
            .claim("type", "refresh")
            .build();

    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

    return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
  }

  @Override
  public Jwt parse(String token, String expectedType) {
    try {
      Jwt decoded = jwtDecoder.decode(token);
      String type = decoded.getClaim("type");
      if (expectedType.equals(type)) {
        return decoded;
      } else {
        throw new InvalidTokenException("Token type mismatch");
      }
    } catch (JwtException e) {
      throw new InvalidTokenException("Invalid token");
    }
  }
}
