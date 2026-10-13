package br.com.clinics.identity.application;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import br.com.clinics.identity.application.port.in.RegisterUserUseCase;
import br.com.clinics.identity.application.port.out.PasswordHasher;
import br.com.clinics.identity.application.port.out.TokenIssuer;
import br.com.clinics.identity.application.port.out.UserRepository;
import br.com.clinics.identity.domain.Cpf;
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
        Cpf cpf = Cpf.of(command.cpf());
        PasswordPolicy.validate(command.password());

        if (users.existsByPhone(phone)) {
            throw new ConflictException("PHONE_ALREADY_REGISTERED", "Este telefone já possui uma conta.");
        }

        if (users.existsByCpf(cpf)) {
            throw new ConflictException("CPF_ALREADY_REGISTERED", "Este CPF já possui uma conta.");
        }

        LocalDate today = LocalDate.now(clock.withZone(ZoneId.of("America/Recife")));
        User candidate = User.register(phone, cpf, command.fullName(), command.email(), command.birthDate(),
                hasher.hash(command.password()), today, clock.instant());
        if (users.existsByEmailExcluding(candidate.email(), candidate.id())) {
            throw new ConflictException("EMAIL_ALREADY_IN_USE", "Este e-mail já está em uso.");
        }
        User user = users.save(candidate);
        return new AuthResult(user, tokens.issue(user));
    }
}
