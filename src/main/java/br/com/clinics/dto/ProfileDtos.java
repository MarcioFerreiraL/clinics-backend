package br.com.clinics.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import br.com.clinics.model.Role;
import br.com.clinics.model.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class ProfileDtos {

    private ProfileDtos() {
    }

    public record ProfileResponse(UUID id, String fullName, String phone, String cpf, String email, LocalDate birthDate, String avatarUrl,
            Role role, Instant createdAt) {

        public static ProfileResponse from(User u) {
            return new ProfileResponse(u.id(), u.fullName(), u.phone().formatted(),
                    u.cpf() == null ? null : u.cpf().formatted(), u.email(), u.birthDate(),
                    u.avatarUrl(), u.role(), u.createdAt());
        }
    }

    /** O telefone não aparece de propósito: não é editável pelo perfil (RN32). */
    public record UpdateProfileRequest(
            @NotBlank(message = "Informe seu nome completo.") @Size(max = 120, message = "Nome muito longo.") String fullName,
            @Size(max = 255, message = "E-mail muito longo.") String email,
            LocalDate birthDate) {
    }

    public record ChangePasswordRequest(
            @NotBlank(message = "Informe a senha atual.") String currentPassword,
            @NotBlank(message = "Informe a nova senha.") String newPassword) {
    }
}
