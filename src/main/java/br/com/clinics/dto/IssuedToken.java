package br.com.clinics.dto;

import java.time.Instant;
import br.com.clinics.model.User;

public record IssuedToken(String value, Instant expiresAt) {
}
