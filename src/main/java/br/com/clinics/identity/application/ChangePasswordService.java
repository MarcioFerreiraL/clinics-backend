package br.com.clinics.identity.application;

import java.time.Clock;

import br.com.clinics.identity.application.port.in.ChangePasswordUseCase;
import br.com.clinics.identity.application.port.out.PasswordHasher;
import br.com.clinics.identity.application.port.out.UserRepository;
import br.com.clinics.identity.domain.PasswordPolicy;
import br.com.clinics.identity.domain.User;
import br.com.clinics.shared.domain.NotFoundException;
import br.com.clinics.shared.domain.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ChangePasswordService implements ChangePasswordUseCase {

    private final UserRepository users;
    private final PasswordHasher hasher;
    private final Clock clock;

    ChangePasswordService(UserRepository users, PasswordHasher hasher, Clock clock) {
        this.users = users;
        this.hasher = hasher;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordCommand command) {
        User user = users.findById(command.userId())
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "Usuário não encontrado."));

        if (command.currentPassword() == null || !hasher.matches(command.currentPassword(), user.passwordHash())) {
            throw new ValidationException("INVALID_CURRENT_PASSWORD", "A senha atual está incorreta.");
        }
        PasswordPolicy.validate(command.newPassword());
        if (hasher.matches(command.newPassword(), user.passwordHash())) {
            throw new ValidationException("SAME_PASSWORD", "A nova senha deve ser diferente da atual.");
        }

        user.changePasswordHash(hasher.hash(command.newPassword()), clock.instant());
        users.save(user);
    }
}
