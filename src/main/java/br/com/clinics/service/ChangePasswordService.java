package br.com.clinics.service;

import br.com.clinics.dto.ChangePasswordCommand;

import java.time.Clock;

import br.com.clinics.service.ChangePasswordService;
import br.com.clinics.security.BcryptPasswordHasher;
import br.com.clinics.repository.UserRepository;
import br.com.clinics.model.PasswordPolicy;
import br.com.clinics.model.User;
import br.com.clinics.exception.NotFoundException;
import br.com.clinics.exception.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChangePasswordService {

    private final UserRepository users;
    private final BcryptPasswordHasher hasher;
    private final Clock clock;

    public ChangePasswordService(UserRepository users, BcryptPasswordHasher hasher, Clock clock) {
        this.users = users;
        this.hasher = hasher;
        this.clock = clock;
    }
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
