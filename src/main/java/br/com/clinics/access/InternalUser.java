package br.com.clinics.access;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Servidor interno: o cargo dele e a clínica em que está lotado (nula para o Administrador). */
@Entity
@Table(name = "internal_users")
public class InternalUser {

    @Id
    private UUID userId;
    private UUID positionId;
    private UUID clinicId;

    protected InternalUser() {
    }

    InternalUser(UUID userId, UUID positionId, UUID clinicId) {
        this.userId = userId;
        this.positionId = positionId;
        this.clinicId = clinicId;
    }

    void assign(UUID positionId, UUID clinicId) {
        this.positionId = positionId;
        this.clinicId = clinicId;
    }

    public UUID userId() {
        return userId;
    }

    public UUID positionId() {
        return positionId;
    }

    public UUID clinicId() {
        return clinicId;
    }
}
