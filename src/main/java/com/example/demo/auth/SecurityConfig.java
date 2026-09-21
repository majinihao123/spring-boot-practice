package com.example.demo.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Configuration
public class SecurityConfig {

    @Bean
    public SecretKey jwtSecretKey(
            @Value("${app.jwt.secret}") String encodedSecret
    ) {
        byte[] secretBytes = Base64.getDecoder().decode(encodedSecret);

        if (secretBytes.length < 32) {
            throw new IllegalArgumentException(
                    "JWT_SECRET 解码后至少需要 32 字节"
            );
        }

        return new SecretKeySpec(secretBytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey secretKey) {
        return NimbusJwtEncoder.withSecretKey(secretKey)
                .algorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    public JwtDecoder jwtDecoder(
            SecretKey secretKey,
            @Value("${app.jwt.issuer}") String issuer
    ) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        decoder.setJwtValidator(
                JwtValidators.createDefaultWithIssuer(issuer)
        );

        return decoder;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                .sessionCreationPolicy(
                        SessionCreationPolicy.STATELESS
                )
                )
                .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(
                        HttpMethod.POST,
                        "/auth/login"
                ).permitAll()
                .requestMatchers(
                        HttpMethod.GET,
                        "/welcome"
                ).permitAll()
                .requestMatchers("/error").permitAll()
                .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(Customizer.withDefaults())
                )
                .build();
    }
}
