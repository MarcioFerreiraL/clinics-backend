package br.com.clinics.identity.infrastructure.web;

import java.time.Instant;
import java.util.UUID;

import br.com.clinics.identity.application.AuthResult;
import br.com.clinics.identity.domain.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

final class AuthDtos {

    private AuthDtos() {
    }

    record RegisterRequest(
            @NotBlank(message = "Informe seu nome completo.") @Size(max = 120, message = "Nome muito longo.") String fullName,
            @NotBlank(message = "Informe seu celular.") String phone,
            @NotBlank(message = "Informe uma senha.") String password) {
    }

    record LoginRequest(
            @NotBlank(message = "Informe seu celular.") String phone,
            @NotBlank(message = "Informe sua senha.") String password) {
    }

    record UserSummary(UUID id, String fullName, String phone, Role role) {
    }

    record AuthResponse(String token, String tokenType, Instant expiresAt, UserSummary user) {

        static AuthResponse from(AuthResult result) {
            var u = result.user();
            return new AuthResponse(result.token().value(), "Bearer", result.token().expiresAt(),
                    new UserSummary(u.id(), u.fullName(), u.phone().formatted(), u.role()));
        }
    }
}
