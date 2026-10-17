package br.com.clinics.service;

import br.com.clinics.dto.AuthResult;
import br.com.clinics.dto.AuthenticateCommand;

import br.com.clinics.service.AuthenticateService;
import br.com.clinics.security.BcryptPasswordHasher;
import br.com.clinics.security.JwtTokenIssuer;
import br.com.clinics.repository.UserRepository;
import br.com.clinics.model.Phone;
import br.com.clinics.model.User;
import br.com.clinics.exception.AuthenticationFailedException;
import br.com.clinics.exception.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticateService {

    private final UserRepository users;
    private final BcryptPasswordHasher hasher;
    private final JwtTokenIssuer tokens;
    /** Hash descartável: igualar o tempo de resposta quando o telefone não existe (RN06). */
    private final String dummyHash;

    public AuthenticateService(UserRepository users, BcryptPasswordHasher hasher, JwtTokenIssuer tokens) {
        this.users = users;
        this.hasher = hasher;
        this.tokens = tokens;
        this.dummyHash = hasher.hash("senha-descartavel-1");
    }
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
