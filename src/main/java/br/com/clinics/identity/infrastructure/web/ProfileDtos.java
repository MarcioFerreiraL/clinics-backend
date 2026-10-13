package br.com.clinics.identity.infrastructure.web;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import br.com.clinics.identity.domain.Role;
import br.com.clinics.identity.domain.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

final class ProfileDtos {

    private ProfileDtos() {
    }

    record ProfileResponse(UUID id, String fullName, String phone, String email, LocalDate birthDate, String avatarUrl,
            Role role, Instant createdAt) {

        static ProfileResponse from(User u) {
            return new ProfileResponse(u.id(), u.fullName(), u.phone().formatted(), u.email(), u.birthDate(),
                    u.avatarUrl(), u.role(), u.createdAt());
        }
    }

    /** O telefone não aparece de propósito: não é editável pelo perfil (RN32). */
    record UpdateProfileRequest(
            @NotBlank(message = "Informe seu nome completo.") @Size(max = 120, message = "Nome muito longo.") String fullName,
            @Size(max = 255, message = "E-mail muito longo.") String email,
            LocalDate birthDate) {
    }

    record ChangePasswordRequest(
            @NotBlank(message = "Informe a senha atual.") String currentPassword,
            @NotBlank(message = "Informe a nova senha.") String newPassword) {
    }
}
