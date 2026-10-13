package br.com.clinics.identity.application;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

import br.com.clinics.identity.application.port.in.ManageProfileUseCase;
import br.com.clinics.identity.application.port.out.AvatarStorage;
import br.com.clinics.identity.application.port.out.UserRepository;
import br.com.clinics.identity.domain.AvatarImage;
import br.com.clinics.identity.domain.User;
import br.com.clinics.shared.domain.ConflictException;
import br.com.clinics.shared.domain.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ManageProfileService implements ManageProfileUseCase {

    private final UserRepository users;
    private final AvatarStorage avatars;
    private final Clock clock;

    ManageProfileService(UserRepository users, AvatarStorage avatars, Clock clock) {
        this.users = users;
        this.avatars = avatars;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public User getProfile(UUID userId) {
        return load(userId);
    }

    @Override
    @Transactional
    public User updateProfile(UpdateProfileCommand command) {
        User user = load(command.userId());
        user.updateProfile(command.fullName(), command.email(), command.birthDate(), LocalDate.now(clock), clock.instant());

        if (user.email() != null && users.existsByEmailExcluding(user.email(), user.id())) {
            throw new ConflictException("EMAIL_ALREADY_IN_USE", "Este e-mail já está em uso por outra conta.");
        }
        return users.save(user);
    }

    @Override
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
