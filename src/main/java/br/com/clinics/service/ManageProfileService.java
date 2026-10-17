package br.com.clinics.service;

import br.com.clinics.dto.UpdateProfileCommand;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

import br.com.clinics.service.ManageProfileService;
import br.com.clinics.storage.LocalAvatarStorage;
import br.com.clinics.repository.UserRepository;
import br.com.clinics.model.AvatarImage;
import br.com.clinics.model.User;
import br.com.clinics.exception.ConflictException;
import br.com.clinics.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ManageProfileService {

    private final UserRepository users;
    private final LocalAvatarStorage avatars;
    private final Clock clock;

    public ManageProfileService(UserRepository users, LocalAvatarStorage avatars, Clock clock) {
        this.users = users;
        this.avatars = avatars;
        this.clock = clock;
    }
    @Transactional(readOnly = true)
    public User getProfile(UUID userId) {
        return load(userId);
    }
    @Transactional
    public User updateProfile(UpdateProfileCommand command) {
        User user = load(command.userId());
        user.updateProfile(command.fullName(), command.email(), command.birthDate(), LocalDate.now(clock), clock.instant());

        if (user.email() != null && users.existsByEmailExcluding(user.email(), user.id())) {
            throw new ConflictException("EMAIL_ALREADY_IN_USE", "Este e-mail já está em uso por outra conta.");
        }
        return users.save(user);
    }
    @Transactional
    public User changeAvatar(UUID userId, byte[] imageBytes) {
        AvatarImage image = AvatarImage.of(imageBytes);
        User user = load(userId);

        String previous = user.avatarUrl();
        user.changeAvatar(avatars.save(userId, image), clock.instant());
        User saved = users.save(user);

        if (previous != null) {
            avatars.delete(previous);
        }
        return saved;
    }

    private User load(UUID userId) {
        return users.findById(userId).orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "Usuário não encontrado."));
    }
}
