package br.com.clinics.identity.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.regex.Pattern;
import java.util.UUID;

import br.com.clinics.shared.domain.ValidationException;

public class User {

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final UUID id;
    private final Phone phone;
    private final Cpf cpf;
    private String email;
    private String passwordHash;
    private String fullName;
    private final Role role;
    private LocalDate birthDate;
    private String avatarUrl;
    private final Instant createdAt;
    private Instant updatedAt;

    public User(UUID id, Phone phone, Cpf cpf, String email, String passwordHash, String fullName, Role role,
            LocalDate birthDate, String avatarUrl, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.phone = phone;
        this.cpf = cpf;
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
        this.birthDate = birthDate;
        this.avatarUrl = avatarUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** Cadastro público: o papel é sempre {@link Role#PATIENT} (RN05). CPF, e-mail e nascimento são obrigatórios (RN37). */
    public static User register(Phone phone, Cpf cpf, String fullName, String email, LocalDate birthDate,
            String passwordHash, LocalDate today, Instant now) {
        if (fullName == null || fullName.isBlank()) {
            throw new ValidationException("INVALID_NAME", "Informe seu nome completo.");
        }
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null) {
            throw new ValidationException("INVALID_EMAIL", "Informe um e-mail válido.");
        }
        validateBirthDate(birthDate, today);
        if (birthDate == null) {
            throw new ValidationException("INVALID_BIRTH_DATE", "Informe sua data de nascimento.");
        }
        return new User(UUID.randomUUID(), phone, cpf, normalizedEmail, passwordHash, fullName.strip(), Role.PATIENT,
                birthDate, null, now, now);
    }

    private static String normalizeEmail(String email) {
        String normalized = email == null || email.isBlank() ? null : email.strip().toLowerCase();
        if (normalized != null && (normalized.length() > 255 || !EMAIL.matcher(normalized).matches())) {
            throw new ValidationException("INVALID_EMAIL", "Informe um e-mail válido.");
        }
        return normalized;
    }

    private static void validateBirthDate(LocalDate birthDate, LocalDate today) {
        if (birthDate != null && birthDate.isAfter(today)) {
            throw new ValidationException("INVALID_BIRTH_DATE", "A data de nascimento não pode estar no futuro.");
        }
    }

    /** RN32 e RN33: nome, e-mail e data de nascimento são editáveis; o telefone não é. */
    public void updateProfile(String newFullName, String newEmail, LocalDate newBirthDate, LocalDate today, Instant now) {
        if (newFullName == null || newFullName.isBlank()) {
            throw new ValidationException("INVALID_NAME", "Informe seu nome completo.");
        }
        String normalizedEmail = normalizeEmail(newEmail);
        validateBirthDate(newBirthDate, today);
        this.fullName = newFullName.strip();
        this.email = normalizedEmail;
        this.birthDate = newBirthDate;
        this.updatedAt = now;
    }

    public void changePasswordHash(String newHash, Instant now) {
        this.passwordHash = newHash;
        this.updatedAt = now;
    }

    public void changeAvatar(String newAvatarUrl, Instant now) {
        this.avatarUrl = newAvatarUrl;
        this.updatedAt = now;
    }

    public UUID id() {
        return id;
    }

    public Phone phone() {
        return phone;
    }

    public Cpf cpf() {
        return cpf;
    }

    public String email() {
        return email;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public String fullName() {
        return fullName;
    }

    public Role role() {
        return role;
    }

    public LocalDate birthDate() {
        return birthDate;
    }

    public String avatarUrl() {
        return avatarUrl;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
