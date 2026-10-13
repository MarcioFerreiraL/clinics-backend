package br.com.clinics.identity.infrastructure.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(String secret, Duration expiration) {

    public JwtProperties {
        if (secret == null || secret.getBytes().length < 32) {
            throw new IllegalStateException("app.security.jwt.secret deve ter ao menos 32 bytes (HS256).");
        }
        if (expiration == null) {
            expiration = Duration.ofHours(1);
        }
    }
}
