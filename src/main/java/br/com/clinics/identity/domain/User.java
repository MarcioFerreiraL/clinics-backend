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
    private String email;
    private String passwordHash;
    private String fullName;
    private final Role role;
    private LocalDate birthDate;
    private String avatarUrl;
    private final Instant createdAt;
    private Instant updatedAt;

    public User(UUID id, Phone phone, String email, String passwordHash, String fullName, Role role,
            LocalDate birthDate, String avatarUrl, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.phone = phone;
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
        this.birthDate = birthDate;
        this.avatarUrl = avatarUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** Cadastro público: o papel é sempre {@link Role#PATIENT} (RN05). */
    public static User register(Phone phone, String fullName, String passwordHash, Instant now) {
        if (fullName == null || fullName.isBlank()) {
            throw new ValidationException("INVALID_NAME", "Informe seu nome completo.");
        }
        return new User(UUID.randomUUID(), phone, null, passwordHash, fullName.strip(), Role.PATIENT,
                null, null, now, now);
    }

    /** RN32 e RN33: nome, e-mail e data de nascimento são editáveis; o telefone não é. */
    public void updateProfile(String newFullName, String newEmail, LocalDate newBirthDate, LocalDate today, Instant now) {
        if (newFullName == null || newFullName.isBlank()) {
            throw new ValidationException("INVALID_NAME", "Informe seu nome completo.");
        }
        String normalizedEmail = newEmail == null || newEmail.isBlank() ? null : newEmail.strip().toLowerCase();
        if (normalizedEmail != null && (normalizedEmail.length() > 255 || !EMAIL.matcher(normalizedEmail).matches())) {
            throw new ValidationException("INVALID_EMAIL", "Informe um e-mail válido.");
        }
        if (newBirthDate != null && newBirthDate.isAfter(today)) {
            throw new ValidationException("INVALID_BIRTH_DATE", "A data de nascimento não pode estar no futuro.");
        }
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
