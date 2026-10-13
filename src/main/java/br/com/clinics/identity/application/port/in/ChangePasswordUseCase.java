package br.com.clinics.identity.application.port.in;

import java.util.UUID;

/** UC12 · Alterar senha. */
public interface ChangePasswordUseCase {

    void changePassword(ChangePasswordCommand command);

    record ChangePasswordCommand(UUID userId, String currentPassword, String newPassword) {
    }
}
