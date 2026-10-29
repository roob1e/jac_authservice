package by.assxmblxr.authservice.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import by.assxmblxr.authservice.configuration.JwtConfig;
import by.assxmblxr.authservice.entity.Credentials;
import by.assxmblxr.authservice.entity.Role;
import by.assxmblxr.authservice.exception.InvalidTokenException;
import com.nimbusds.jose.jwk.RSAKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.time.Duration;
import java.time.Instant;

class TokenServiceImplTest {
  private static final Duration ACCESS_TTL = Duration.ofMinutes(15);
  private static final Duration REFRESH_TTL = Duration.ofDays(7);

  private final JwtConfig jwtConfig = new JwtConfig();
  private RSAKey rsaKey;
  private TokenServiceImpl tokenService;

  @BeforeEach
  void setUp() throws Exception {
    rsaKey = jwtConfig.rsaKey();
    tokenService = tokenService(ACCESS_TTL, rsaKey);
  }

  @Test
  void accessTokenIsSignedWithRs256AndContainsUserIdAndRole() throws Exception {
    Instant before = Instant.now().minusSeconds(1);

    String token = tokenService.generateAccessToken(credentials());

    Jwt jwt = publicKeyDecoder(rsaKey).decode(token);
    assertThat(jwt.getHeaders()).containsEntry("alg", "RS256").containsEntry("kid", rsaKey.getKeyID());
    assertThat(jwt.getSubject()).isEqualTo("ivan_petrov");
    assertThat(jwt.<Long>getClaim("userId")).isEqualTo(42L);
    assertThat(jwt.getClaimAsString("role")).isEqualTo("USER");
    assertThat(jwt.getClaimAsString("type")).isEqualTo("access");
    assertThat(jwt.getIssuedAt()).isAfter(before);
    assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(ACCESS_TTL);
  }

  @Test
  void refreshTokenHasRefreshTypeLongerLifetimeAndNoRole() throws Exception {
    String token = tokenService.generateRefreshToken(credentials());

    Jwt jwt = publicKeyDecoder(rsaKey).decode(token);
    assertThat(jwt.getHeaders()).containsEntry("alg", "RS256");
    assertThat(jwt.getSubject()).isEqualTo("ivan_petrov");
    assertThat(jwt.<Long>getClaim("userId")).isEqualTo(42L);
    assertThat(jwt.getClaimAsString("type")).isEqualTo("refresh");
    assertThat(jwt.hasClaim("role")).isFalse();
    assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(REFRESH_TTL);
  }

  @Test
  void parseReturnsTokenOfExpectedType() {
    String token = tokenService.generateAccessToken(credentials());

    Jwt jwt = tokenService.parse(token, "access");

    assertThat(jwt.getSubject()).isEqualTo("ivan_petrov");
  }

  @Test
  void parseRejectsTokenOfAnotherType() {
    String refreshToken = tokenService.generateRefreshToken(credentials());

    assertThatThrownBy(() -> tokenService.parse(refreshToken, "access"))
            .isInstanceOf(InvalidTokenException.class)
            .hasMessage("Token type mismatch");
  }

  @Test
  void parseRejectsTamperedToken() {
    String token = tokenService.generateAccessToken(credentials());
    String tampered = token.substring(0, token.length() - 2) + (token.endsWith("AA") ? "BB" : "AA");

    assertThatThrownBy(() -> tokenService.parse(tampered, "access"))
            .isInstanceOf(InvalidTokenException.class)
            .hasMessage("Invalid token");
  }

  @Test
  void parseRejectsTokenSignedWithAnotherKey() throws Exception {
    TokenServiceImpl foreignIssuer = tokenService(ACCESS_TTL, jwtConfig.rsaKey());
    String foreignToken = foreignIssuer.generateAccessToken(credentials());

    assertThatThrownBy(() -> tokenService.parse(foreignToken, "access"))
            .isInstanceOf(InvalidTokenException.class)
            .hasMessage("Invalid token");
  }

  @Test
  void parseRejectsExpiredToken() throws Exception {
    Instant issuedAt = Instant.now().minus(Duration.ofMinutes(10));
    JwtClaimsSet claims = JwtClaimsSet.builder()
            .subject("ivan_petrov")
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plus(Duration.ofMinutes(5)))
            .claim("type", "access")
            .build();
    String expiredToken = jwtConfig.jwtEncoder(rsaKey)
            .encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), claims))
            .getTokenValue();

    assertThatThrownBy(() -> tokenService.parse(expiredToken, "access"))
            .isInstanceOf(InvalidTokenException.class)
            .hasMessage("Invalid token");
  }

  @Test
  void parseRejectsMalformedToken() {
    assertThatThrownBy(() -> tokenService.parse("not.a.jwt", "access"))
            .isInstanceOf(InvalidTokenException.class);
  }

  private TokenServiceImpl tokenService(Duration accessTtl, RSAKey key) throws Exception {
    return new TokenServiceImpl(accessTtl, REFRESH_TTL, jwtConfig.jwtEncoder(key), jwtConfig.jwtDecoder(rsaKey));
  }

  private JwtDecoder publicKeyDecoder(RSAKey key) throws Exception {
    return NimbusJwtDecoder.withPublicKey(key.toRSAPublicKey())
            .signatureAlgorithm(SignatureAlgorithm.RS256)
            .build();
  }

  private Credentials credentials() {
    return Credentials.builder()
            .id(1L)
            .userId(42L)
            .username("ivan_petrov")
            .passwordHash("hash")
            .role(Role.USER)
            .build();
  }
}
