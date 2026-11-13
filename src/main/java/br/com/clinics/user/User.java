package br.com.clinics.user;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Telefone e CPF ficam só com dígitos e não mudam depois do cadastro. */
@Entity
@Table(name = "users")
public class User {

    @Id
    private UUID id;
    @Column(updatable = false)
    private String phone;
    @Column(updatable = false)
    private String cpf;
    private String email;
    private String passwordHash;
    private String fullName;
    @Column(updatable = false)
    private String role;
    private LocalDate birthDate;
    private String avatarUrl;
    @Column(updatable = false)
    private Instant createdAt;
    private Instant updatedAt;

    protected User() {
    }

    User(String phone, String cpf, String email, String passwordHash, String fullName, LocalDate birthDate, Instant now) {
        this.id = UUID.randomUUID();
        this.phone = phone;
        this.cpf = cpf;
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = "PATIENT";
        this.birthDate = birthDate;
        this.createdAt = now;
        this.updatedAt = now;
    }

    void updateProfile(String fullName, String email, LocalDate birthDate, Instant now) {
        this.fullName = fullName;
        this.email = email;
        this.birthDate = birthDate;
        this.updatedAt = now;
    }

    void changeAvatar(String avatarUrl, Instant now) {
        this.avatarUrl = avatarUrl;
        this.updatedAt = now;
    }

    void changePassword(String passwordHash, Instant now) {
        this.passwordHash = passwordHash;
        this.updatedAt = now;
    }

    public UUID id() {
        return id;
    }

    public String phone() {
        return phone;
    }

    public String cpf() {
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

    public String role() {
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
}
