package br.com.clinics.service;

import br.com.clinics.dto.AuthResult;
import br.com.clinics.dto.RegisterUserCommand;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import br.com.clinics.service.RegisterUserService;
import br.com.clinics.security.BcryptPasswordHasher;
import br.com.clinics.security.JwtTokenIssuer;
import br.com.clinics.repository.UserRepository;
import br.com.clinics.model.Cpf;
import br.com.clinics.model.PasswordPolicy;
import br.com.clinics.model.Phone;
import br.com.clinics.model.User;
import br.com.clinics.exception.ConflictException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterUserService {

    private final UserRepository users;
    private final BcryptPasswordHasher hasher;
    private final JwtTokenIssuer tokens;
    private final Clock clock;

    public RegisterUserService(UserRepository users, BcryptPasswordHasher hasher, JwtTokenIssuer tokens, Clock clock) {
        this.users = users;
        this.hasher = hasher;
        this.tokens = tokens;
        this.clock = clock;
    }
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
