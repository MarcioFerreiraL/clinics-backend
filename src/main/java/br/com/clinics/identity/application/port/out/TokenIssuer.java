package br.com.clinics.identity.application.port.out;

import java.time.Instant;

import br.com.clinics.identity.domain.User;

public interface TokenIssuer {

    IssuedToken issue(User user);

    record IssuedToken(String value, Instant expiresAt) {
    }
}
