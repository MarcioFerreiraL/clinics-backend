package br.com.clinics.access;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface InternalUserRepository extends JpaRepository<InternalUser, UUID> {

    /** Usuário → cargo → permissões, já no formato recurso.ação. */
    @Query(value = """
            SELECT m.resource || '.' || m.action FROM internal_users i
            JOIN position_permissions pp ON pp.position_id = i.position_id
            JOIN permissions m ON m.id = pp.permission_id
            WHERE i.user_id = :userId""", nativeQuery = true)
    List<String> findPermissionKeys(UUID userId);

    @Query(value = """
            SELECT p.visibility_scope FROM internal_users i JOIN positions p ON p.id = i.position_id
            WHERE i.user_id = :userId""", nativeQuery = true)
    String findVisibilityScope(UUID userId);

    @Query(value = """
            SELECT u.id AS id, u.full_name AS fullName, u.phone AS phone, u.role AS role,
                   p.id AS positionId, p.name AS position, i.clinic_id AS clinicId, c.name AS clinicName
            FROM users u
            JOIN internal_users i ON i.user_id = u.id
            JOIN positions p ON p.id = i.position_id
            LEFT JOIN clinics c ON c.id = i.clinic_id
            ORDER BY u.full_name""", nativeQuery = true)
    List<AccountRow> findAccounts();

    /** O papel do token acompanha o cargo: Administrador vira ADMIN, os demais CLINIC_MANAGER. */
    @Modifying
    @Query(value = "UPDATE users SET role = :role WHERE id = :userId", nativeQuery = true)
    int updateRole(UUID userId, String role);

    interface AccountRow {

        UUID getId();

        String getFullName();

        String getPhone();

        String getRole();

        UUID getPositionId();

        String getPosition();

        UUID getClinicId();

        String getClinicName();
    }
}
