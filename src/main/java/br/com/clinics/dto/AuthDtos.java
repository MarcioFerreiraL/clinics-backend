package br.com.clinics.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import br.com.clinics.dto.AuthResult;
import br.com.clinics.model.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank(message = "Informe seu nome completo.") @Size(max = 120, message = "Nome muito longo.") String fullName,
            @NotBlank(message = "Informe seu celular.") String phone,
            @NotBlank(message = "Informe seu CPF.") String cpf,
            @NotBlank(message = "Informe seu e-mail.") @Size(max = 255, message = "E-mail muito longo.") String email,
            @NotNull(message = "Informe sua data de nascimento.") LocalDate birthDate,
            @NotBlank(message = "Informe uma senha.") String password) {
    }

    public record LoginRequest(
            @NotBlank(message = "Informe seu celular.") String phone,
            @NotBlank(message = "Informe sua senha.") String password) {
    }

    public record UserSummary(UUID id, String fullName, String phone, Role role) {
    }

    public record AuthResponse(String token, String tokenType, Instant expiresAt, UserSummary user) {

        public static AuthResponse from(AuthResult result) {
            var u = result.user();
            return new AuthResponse(result.token().value(), "Bearer", result.token().expiresAt(),
                    new UserSummary(u.id(), u.fullName(), u.phone().formatted(), u.role()));
        }
    }
}
