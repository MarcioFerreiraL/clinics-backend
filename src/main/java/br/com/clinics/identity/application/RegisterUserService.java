package br.com.clinics.identity.application;

import java.time.Clock;

import br.com.clinics.identity.application.port.in.RegisterUserUseCase;
import br.com.clinics.identity.application.port.out.PasswordHasher;
import br.com.clinics.identity.application.port.out.TokenIssuer;
import br.com.clinics.identity.application.port.out.UserRepository;
import br.com.clinics.identity.domain.PasswordPolicy;
import br.com.clinics.identity.domain.Phone;
import br.com.clinics.identity.domain.User;
import br.com.clinics.shared.domain.ConflictException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class RegisterUserService implements RegisterUserUseCase {

    private final UserRepository users;
    private final PasswordHasher hasher;
    private final TokenIssuer tokens;
    private final Clock clock;

    RegisterUserService(UserRepository users, PasswordHasher hasher, TokenIssuer tokens, Clock clock) {
        this.users = users;
        this.hasher = hasher;
        this.tokens = tokens;
        this.clock = clock;
    }

    @Override
    @Transactional
    public AuthResult register(RegisterUserCommand command) {
        Phone phone = Phone.of(command.phone());
        PasswordPolicy.validate(command.password());

        if (users.existsByPhone(phone)) {
            throw new ConflictException("PHONE_ALREADY_REGISTERED", "Este telefone já possui uma conta.");
        }

        User user = users.save(User.register(phone, command.fullName(), hasher.hash(command.password()), clock.instant()));
        return new AuthResult(user, tokens.issue(user));
    }
}
