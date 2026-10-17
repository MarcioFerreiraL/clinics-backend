package br.com.clinics.dto;

import br.com.clinics.dto.IssuedToken;
import br.com.clinics.model.User;

public record AuthResult(User user, IssuedToken token) {
}
