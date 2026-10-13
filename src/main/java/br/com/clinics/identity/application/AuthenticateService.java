package br.com.clinics.identity.application;

import br.com.clinics.identity.application.port.in.AuthenticateUseCase;
import br.com.clinics.identity.application.port.out.PasswordHasher;
import br.com.clinics.identity.application.port.out.TokenIssuer;
import br.com.clinics.identity.application.port.out.UserRepository;
import br.com.clinics.identity.domain.Phone;
import br.com.clinics.identity.domain.User;
import br.com.clinics.shared.domain.AuthenticationFailedException;
import br.com.clinics.shared.domain.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AuthenticateService implements AuthenticateUseCase {

    private final UserRepository users;
    private final PasswordHasher hasher;
    private final TokenIssuer tokens;
    /** Hash descartável: igualar o tempo de resposta quando o telefone não existe (RN06). */
    private final String dummyHash;

    AuthenticateService(UserRepository users, PasswordHasher hasher, TokenIssuer tokens) {
        this.users = users;
        this.hasher = hasher;
        this.tokens = tokens;
        this.dummyHash = hasher.hash("senha-descartavel-1");
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResult authenticate(AuthenticateCommand command) {
        User user = findUser(command.phone());
        String hash = user != null ? user.passwordHash() : dummyHash;
        boolean passwordMatches = command.password() != null && hasher.matches(command.password(), hash);

        if (user == null || !passwordMatches) {
            throw new AuthenticationFailedException();
        }
        return new AuthResult(user, tokens.issue(user));
    }

    private User findUser(String rawPhone) {
        try {
            return users.findByPhone(Phone.of(rawPhone)).orElse(null);
        } catch (ValidationException invalidPhone) {
            return null;
        }
    }
}
