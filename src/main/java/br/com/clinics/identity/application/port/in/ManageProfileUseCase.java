package br.com.clinics.identity.application.port.in;

import java.time.LocalDate;
import java.util.UUID;

import br.com.clinics.identity.domain.User;

/** UC11 · Gerenciar perfil. O id do usuário sempre vem do token, nunca do cliente (RN35). */
public interface ManageProfileUseCase {

    User getProfile(UUID userId);

    User updateProfile(UpdateProfileCommand command);

    User changeAvatar(UUID userId, byte[] imageBytes);

    record UpdateProfileCommand(UUID userId, String fullName, String email, LocalDate birthDate) {
    }
}
