package br.com.clinics.user;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import br.com.clinics.common.ApiException;
import br.com.clinics.common.WebConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cadastro, login, perfil, foto e senha do usuário. */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final int MAX_AVATAR_BYTES = 2 * 1024 * 1024;

    record Token(String value, Instant expiresAt) {
    }

    private final UserRepository users;
    private final BCryptPasswordEncoder passwords;
    private final JwtEncoder jwtEncoder;
    private final Duration tokenExpiration;
    private final Path avatarsDir;
    private final Clock clock;
    /** Comparado quando o telefone não existe, para o tempo de resposta não revelar quem tem conta. */
    private final String dummyHash;

    public UserService(UserRepository users, BCryptPasswordEncoder passwords, JwtEncoder jwtEncoder,
            Duration tokenExpiration, Path avatarsDir, Clock clock) {
        this.users = users;
        this.passwords = passwords;
        this.jwtEncoder = jwtEncoder;
        this.tokenExpiration = tokenExpiration;
        this.avatarsDir = avatarsDir;
        this.clock = clock;
        this.dummyHash = passwords.encode("senha-descartavel-1");
    }

    // ---- cadastro e login ----

    @Transactional
    public User register(String fullName, String rawPhone, String rawCpf, String rawEmail, LocalDate birthDate,
            String password) {
        String phone = parsePhone(rawPhone);
        String cpf = parseCpf(rawCpf);
        validatePassword(password);
        if (users.existsByPhone(phone)) {
            throw ApiException.conflict("PHONE_ALREADY_REGISTERED", "Este telefone já possui uma conta.");
        }
        if (users.existsByCpf(cpf)) {
            throw ApiException.conflict("CPF_ALREADY_REGISTERED", "Este CPF já possui uma conta.");
        }
        String name = requireName(fullName);
        String email = normalizeEmail(rawEmail);
        if (email == null) {
            throw ApiException.badRequest("INVALID_EMAIL", "Informe um e-mail válido.");
        }
        validateBirthDate(birthDate);
        if (birthDate == null) {
            throw ApiException.badRequest("INVALID_BIRTH_DATE", "Informe sua data de nascimento.");
        }
        if (users.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("EMAIL_ALREADY_IN_USE", "Este e-mail já está em uso.");
        }
        return users.save(new User(phone, cpf, email, passwords.encode(password), name, birthDate, clock.instant()));
    }

    @Transactional(readOnly = true)
    public User login(String rawPhone, String password) {
        User user = findByPhone(rawPhone).orElse(null);
        String hash = user != null ? user.passwordHash() : dummyHash;
        boolean matches = password != null && passwords.matches(password, hash);
        if (user == null || !matches) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
                    "Telefone ou senha inválidos.");
        }
        return user;
    }

    public Token issueToken(User user) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(tokenExpiration);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.id().toString())
                .claim("role", user.role())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .build();
        String value = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return new Token(value, expiresAt);
    }

    // ---- perfil ----

    @Transactional(readOnly = true)
    public User find(UUID id) {
        return users.findById(id)
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "Usuário não encontrado."));
    }

    /** Nome, e-mail e nascimento são editáveis; telefone e CPF não. */
    @Transactional
    public User updateProfile(UUID id, String fullName, String rawEmail, LocalDate birthDate) {
        User user = find(id);
        String name = requireName(fullName);
        String email = normalizeEmail(rawEmail);
        validateBirthDate(birthDate);
        if (email != null && users.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw ApiException.conflict("EMAIL_ALREADY_IN_USE", "Este e-mail já está em uso por outra conta.");
        }
        user.updateProfile(name, email, birthDate, clock.instant());
        return users.save(user);
    }

    /** Somente JPEG ou PNG de até 2 MB; o tipo é detectado pelos bytes, não pelo cabeçalho do cliente. */
    @Transactional
    public User changeAvatar(UUID id, byte[] image) {
        String extension = imageExtension(image);
        User user = find(id);
        String filename = "%s-%s.%s".formatted(id, UUID.randomUUID().toString().substring(0, 8), extension);
        try {
            Files.createDirectories(avatarsDir);
            Files.write(avatarsDir.resolve(filename), image);
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao gravar o avatar", e);
        }
        String previous = user.avatarUrl();
        user.changeAvatar(WebConfig.AVATARS_PREFIX + filename, clock.instant());
        User saved = users.save(user);
        deleteAvatar(previous);
        return saved;
    }

    @Transactional
    public void changePassword(UUID id, String current, String next) {
        User user = find(id);
        if (current == null || !passwords.matches(current, user.passwordHash())) {
            throw ApiException.badRequest("INVALID_CURRENT_PASSWORD", "A senha atual está incorreta.");
        }
        validatePassword(next);
        if (passwords.matches(next, user.passwordHash())) {
            throw ApiException.badRequest("SAME_PASSWORD", "A nova senha deve ser diferente da atual.");
        }
        user.changePassword(passwords.encode(next), clock.instant());
        users.save(user);
    }

    // ---- regras de formato ----

    /** Celular com DDD, guardado com DDI 55: {@code (81) 99876-5432} vira {@code 5581998765432}. */
    static String parsePhone(String raw) {
        String digits = raw == null ? "" : raw.replaceAll("\\D", "");
        if (digits.length() == 11) {
            digits = "55" + digits;
        }
        if (!digits.matches("55[1-9][0-9]9[0-9]{8}")) {
            throw ApiException.badRequest("INVALID_PHONE", "Informe um celular válido com DDD, por exemplo (81) 99876-5432.");
        }
        return digits;
    }

    static String formatPhone(String digits) {
        return "(%s) %s-%s".formatted(digits.substring(2, 4), digits.substring(4, 9), digits.substring(9));
    }

    /** CPF com 11 dígitos e dígitos verificadores válidos. */
    static String parseCpf(String raw) {
        String d = raw == null ? "" : raw.replaceAll("\\D", "");
        boolean valid = d.matches("\\d{11}") && d.chars().distinct().count() > 1
                && cpfDigit(d, 9) == d.charAt(9) - '0' && cpfDigit(d, 10) == d.charAt(10) - '0';
        if (!valid) {
            throw ApiException.badRequest("INVALID_CPF", "Informe um CPF válido.");
        }
        return d;
    }

    private static int cpfDigit(String d, int length) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += (d.charAt(i) - '0') * (length + 1 - i);
        }
        int rest = (sum * 10) % 11;
        return rest == 10 ? 0 : rest;
    }

    static String formatCpf(String d) {
        return d == null ? null : "%s.%s.%s-%s".formatted(d.substring(0, 3), d.substring(3, 6), d.substring(6, 9), d.substring(9));
    }

    /** Ao menos 8 caracteres, com letras e números. */
    static void validatePassword(String password) {
        boolean valid = password != null && password.length() >= 8
                && password.chars().anyMatch(Character::isLetter)
                && password.chars().anyMatch(Character::isDigit);
        if (!valid) {
            throw ApiException.badRequest("WEAK_PASSWORD", "A senha deve ter ao menos 8 caracteres, com letras e números.");
        }
    }

    private static String requireName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            throw ApiException.badRequest("INVALID_NAME", "Informe seu nome completo.");
        }
        return fullName.strip();
    }

    /** E-mail vazio vira {@code null}; preenchido precisa ter formato válido. */
    private static String normalizeEmail(String email) {
        String normalized = email == null || email.isBlank() ? null : email.strip().toLowerCase();
        if (normalized != null && (normalized.length() > 255 || !EMAIL.matcher(normalized).matches())) {
            throw ApiException.badRequest("INVALID_EMAIL", "Informe um e-mail válido.");
        }
        return normalized;
    }

    private void validateBirthDate(LocalDate birthDate) {
        if (birthDate != null && birthDate.isAfter(LocalDate.now(clock))) {
            throw ApiException.badRequest("INVALID_BIRTH_DATE", "A data de nascimento não pode estar no futuro.");
        }
    }

    private static String imageExtension(byte[] data) {
        if (data != null && data.length > 0 && data.length <= MAX_AVATAR_BYTES) {
            if (startsWith(data, (byte) 0x89, (byte) 'P', (byte) 'N', (byte) 'G')) {
                return "png";
            }
            if (startsWith(data, (byte) 0xFF, (byte) 0xD8, (byte) 0xFF)) {
                return "jpg";
            }
        }
        throw ApiException.badRequest("INVALID_AVATAR", "Envie uma imagem JPEG ou PNG de até 2 MB.");
    }

    private static boolean startsWith(byte[] data, byte... prefix) {
        if (data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private Optional<User> findByPhone(String rawPhone) {
        String phone;
        try {
            phone = parsePhone(rawPhone);
        } catch (ApiException invalidPhone) {
            return Optional.empty();
        }
        return users.findByPhone(phone);
    }

    private void deleteAvatar(String publicPath) {
        if (publicPath == null || !publicPath.startsWith(WebConfig.AVATARS_PREFIX)) {
            return;
        }
        Path target = avatarsDir.resolve(publicPath.substring(WebConfig.AVATARS_PREFIX.length())).normalize();
        if (!target.startsWith(avatarsDir)) {
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            log.warn("Não foi possível remover o avatar antigo {}", target, e);
        }
    }
}
