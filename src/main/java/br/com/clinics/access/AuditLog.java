package br.com.clinics.access;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Quem mudou a matriz ou o cargo de uma conta, e quando. */
@Entity
@Table(name = "permission_audit_logs")
public class AuditLog {

    @Id
    private UUID id;
    private UUID actorUserId;
    private String action;
    private UUID positionId;
    private UUID permissionId;
    private UUID targetUserId;
    private Instant createdAt;

    protected AuditLog() {
    }

    AuditLog(UUID actorUserId, String action, UUID positionId, UUID permissionId, UUID targetUserId, Instant now) {
        this.id = UUID.randomUUID();
        this.actorUserId = actorUserId;
        this.action = action;
        this.positionId = positionId;
        this.permissionId = permissionId;
        this.targetUserId = targetUserId;
        this.createdAt = now;
    }
}
