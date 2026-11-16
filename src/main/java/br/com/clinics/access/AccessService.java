package br.com.clinics.access;

import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import br.com.clinics.catalog.ClinicRepository;
import br.com.clinics.common.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Camada 2 do controle de acesso: permissão (cargo → permissões, lidas do banco a cada chamada, então uma mudança
 * na matriz vale na requisição seguinte) e escopo (Administrador vê todas as clínicas; os outros, só a da lotação).
 */
@Service
public class AccessService {

    public static final String ADMIN_POSITION = "Administrador";

    private final InternalUserRepository internalUsers;
    private final PositionRepository positions;
    private final PermissionRepository permissions;
    private final AuditLogRepository audit;
    private final ClinicRepository clinics;
    private final Clock clock;

    public AccessService(InternalUserRepository internalUsers, PositionRepository positions,
            PermissionRepository permissions, AuditLogRepository audit, ClinicRepository clinics, Clock clock) {
        this.internalUsers = internalUsers;
        this.positions = positions;
        this.permissions = permissions;
        this.audit = audit;
        this.clinics = clinics;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Set<String> permissionsOf(UUID userId) {
        return new HashSet<>(internalUsers.findPermissionKeys(userId));
    }

    @Transactional(readOnly = true)
    public boolean hasPermission(UUID userId, String key) {
        return permissionsOf(userId).contains(key);
    }

    /** 403 quando o cargo da conta não tem a permissão. */
    @Transactional(readOnly = true)
    public void require(UUID userId, String key) {
        if (!hasPermission(userId, key)) {
            throw ApiException.forbidden("ACCESS_DENIED", "Você não tem permissão para esta operação.");
        }
    }

    @Transactional(readOnly = true)
    public boolean canAccessClinic(UUID userId, UUID clinicId) {
        String scope = internalUsers.findVisibilityScope(userId);
        if (scope == null) {
            return false;
        }
        if (scope.equals("all")) {
            return true;
        }
        return internalUsers.findById(userId).map(i -> clinicId != null && clinicId.equals(i.clinicId())).orElse(false);
    }

    /** Clínica da lotação da conta; vazio para o Administrador e para quem não é servidor interno. */
    @Transactional(readOnly = true)
    public Optional<UUID> clinicIdOf(UUID userId) {
        return internalUsers.findById(userId).map(InternalUser::clinicId);
    }

    @Transactional(readOnly = true)
    public Optional<Position> positionOf(UUID userId) {
        return internalUsers.findById(userId).flatMap(i -> positions.findById(i.positionId()));
    }

    // ---------- administração ----------

    @Transactional(readOnly = true)
    public List<InternalUserRepository.AccountRow> accounts(UUID actorId) {
        require(actorId, "conta.listar");
        return internalUsers.findAccounts();
    }

    @Transactional
    public void changePosition(UUID actorId, UUID targetUserId, UUID positionId, UUID clinicId) {
        require(actorId, "conta.gerenciar");
        if (actorId.equals(targetUserId)) {
            throw ApiException.badRequest("CANNOT_CHANGE_OWN_POSITION", "Você não pode trocar o próprio cargo.");
        }
        Position position = positions.findById(positionId)
                .orElseThrow(() -> ApiException.notFound("POSITION_NOT_FOUND", "Cargo não encontrado."));
        InternalUser target = internalUsers.findById(targetUserId)
                .orElseThrow(() -> ApiException.notFound("ACCOUNT_NOT_FOUND", "Conta interna não encontrada."));
        boolean admin = position.visibilityScope().equals("all");
        if (!admin && (clinicId == null || !clinics.existsById(clinicId))) {
            throw ApiException.badRequest("CLINIC_REQUIRED", "Informe uma clínica existente para este cargo.");
        }
        target.assign(positionId, admin ? null : clinicId);
        internalUsers.save(target);
        internalUsers.updateRole(targetUserId, admin ? "ADMIN" : "CLINIC_MANAGER");
        audit.save(new AuditLog(actorId, "position_change", positionId, null, targetUserId, clock.instant()));
    }

    public record PositionView(UUID id, String name, String visibilityScope, List<String> permissions) {
    }

    public record PermissionView(UUID id, String key, String label) {
    }

    public record Matrix(List<PositionView> positions, List<PermissionView> permissions) {
    }

    @Transactional(readOnly = true)
    public Matrix matrix(UUID actorId) {
        require(actorId, "permissao.gerenciar");
        List<Permission> all = permissions.findAll();
        List<PositionView> views = positions.findAll().stream().map(p -> {
            Set<UUID> granted = new HashSet<>(positions.findPermissionIds(p.id()));
            List<String> keys = all.stream().filter(m -> granted.contains(m.id())).map(Permission::key).sorted().toList();
            return new PositionView(p.id(), p.name(), p.visibilityScope(), keys);
        }).sorted((a, b) -> a.name().compareTo(b.name())).toList();
        return new Matrix(views,
                all.stream().map(m -> new PermissionView(m.id(), m.key(), m.label())).sorted((a, b) -> a.key().compareTo(b.key())).toList());
    }

    @Transactional
    public void grant(UUID actorId, UUID positionId, UUID permissionId) {
        change(actorId, positionId, permissionId, true);
    }

    @Transactional
    public void revoke(UUID actorId, UUID positionId, UUID permissionId) {
        change(actorId, positionId, permissionId, false);
    }

    private void change(UUID actorId, UUID positionId, UUID permissionId, boolean grant) {
        require(actorId, "permissao.gerenciar");
        Position position = positions.findById(positionId)
                .orElseThrow(() -> ApiException.notFound("POSITION_NOT_FOUND", "Cargo não encontrado."));
        Permission permission = permissions.findById(permissionId)
                .orElseThrow(() -> ApiException.notFound("PERMISSION_NOT_FOUND", "Permissão não encontrada."));
        if (!grant && position.name().equals(ADMIN_POSITION) && permission.key().equals("permissao.gerenciar")) {
            throw ApiException.badRequest("LAST_ADMIN_PERMISSION",
                    "O Administrador não pode perder a permissão de gerenciar permissões.");
        }
        int changed = grant ? positions.grant(positionId, permissionId) : positions.revoke(positionId, permissionId);
        if (changed > 0) {
            audit.save(new AuditLog(actorId, grant ? "grant" : "revoke", positionId, permissionId, null, clock.instant()));
        }
    }
}
