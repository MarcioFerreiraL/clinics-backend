package br.com.clinics.access;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PositionRepository extends JpaRepository<Position, UUID> {

    @Query(value = "SELECT permission_id FROM position_permissions WHERE position_id = :positionId", nativeQuery = true)
    List<UUID> findPermissionIds(UUID positionId);

    @Modifying
    @Query(value = """
            INSERT INTO position_permissions (position_id, permission_id) VALUES (:positionId, :permissionId)
            ON CONFLICT DO NOTHING""", nativeQuery = true)
    int grant(UUID positionId, UUID permissionId);

    @Modifying
    @Query(value = "DELETE FROM position_permissions WHERE position_id = :positionId AND permission_id = :permissionId",
            nativeQuery = true)
    int revoke(UUID positionId, UUID permissionId);
}
