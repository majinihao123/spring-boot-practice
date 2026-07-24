 package com.example.demo.auth;

  import org.springframework.beans.factory.annotation.Value;
  import org.springframework.http.HttpStatus;
  import org.springframework.security.crypto.password.PasswordEncoder;
  import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
  import org.springframework.security.oauth2.jwt.JwtClaimsSet;
  import org.springframework.security.oauth2.jwt.JwtEncoder;
  import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
  import org.springframework.security.oauth2.jwt.JwsHeader;
  import org.springframework.stereotype.Service;
  import org.springframework.transaction.annotation.Transactional;
  import org.springframework.util.StringUtils;
  import org.springframework.web.server.ResponseStatusException;

  import java.time.Duration;
  import java.time.Instant;

  @Service
  @Transactional(readOnly = true)
  public class AuthService {

      private final AppUserRepository userRepository;
      private final PasswordEncoder passwordEncoder;
      private final JwtEncoder jwtEncoder;
      private final String issuer;
      private final Duration expiration;

      public AuthService(
              AppUserRepository userRepository,
              PasswordEncoder passwordEncoder,
              JwtEncoder jwtEncoder,
              @Value("${app.jwt.issuer}") String issuer,
              @Value("${app.jwt.expiration}") Duration expiration
      ) {
          this.userRepository = userRepository;
          this.passwordEncoder = passwordEncoder;
          this.jwtEncoder = jwtEncoder;
          this.issuer = issuer;
          this.expiration = expiration;
      }

      public LoginResponse login(LoginRequest request) {
          if (request == null
                  || !StringUtils.hasText(request.username())
                  || !StringUtils.hasText(request.password())) {
              throw unauthorized();
          }

          AppUser user = userRepository
                  .findByUsername(request.username())
                  .filter(AppUser::isEnabled)
                  .orElseThrow(this::unauthorized);

          if (!passwordEncoder.matches(
                  request.password(),
                  user.getPasswordHash()
          )) {
              throw unauthorized();
          }

          Instant issuedAt = Instant.now();
          Instant expiresAt = issuedAt.plus(expiration);

          JwsHeader header = JwsHeader
                  .with(MacAlgorithm.HS256)
                  .type("JWT")
                  .build();

          JwtClaimsSet claims = JwtClaimsSet.builder()
                  .issuer(issuer)
                  .issuedAt(issuedAt)
                  .expiresAt(expiresAt)
                  .subject(user.getUsername())
                  .claim("role", user.getRole())
                  .build();

          String token = jwtEncoder.encode(
                  JwtEncoderParameters.from(header, claims)
          ).getTokenValue();

          return new LoginResponse(
                  "Bearer",
                  token,
                  expiration.toSeconds()
          );
      }

      private ResponseStatusException unauthorized() {
          return new ResponseStatusException(
                  HttpStatus.UNAUTHORIZED,
                  "用户名或密码错误"
          );
      }
  }