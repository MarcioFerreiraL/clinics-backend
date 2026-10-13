package br.com.clinics.identity.application;

import br.com.clinics.identity.application.port.out.TokenIssuer.IssuedToken;
import br.com.clinics.identity.domain.User;

public record AuthResult(User user, IssuedToken token) {
}
