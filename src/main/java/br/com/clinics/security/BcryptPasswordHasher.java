package br.com.clinics.security;

import br.com.clinics.security.BcryptPasswordHasher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BcryptPasswordHasher {

    private static final int COST = 10;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(COST);
    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }
    public boolean matches(String rawPassword, String hash) {
        return encoder.matches(rawPassword, hash);
    }
}
