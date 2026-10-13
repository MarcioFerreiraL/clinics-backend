package br.com.clinics.identity.infrastructure.persistence;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import br.com.clinics.identity.domain.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
class UserJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, updatable = false)
    private String phone;

    @Column(updatable = false)
    private String cpf;

    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private Role role;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserJpaEntity() {
    }

    UserJpaEntity(UUID id, String phone, String cpf, String email, String passwordHash, String fullName, Role role,
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

    UUID getId() {
        return id;
    }

    String getPhone() {
        return phone;
    }

    String getCpf() {
        return cpf;
    }

    String getEmail() {
        return email;
    }

    String getPasswordHash() {
        return passwordHash;
    }

    String getFullName() {
        return fullName;
    }

    Role getRole() {
        return role;
    }

    LocalDate getBirthDate() {
        return birthDate;
    }

    String getAvatarUrl() {
        return avatarUrl;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    Instant getUpdatedAt() {
        return updatedAt;
    }
}
