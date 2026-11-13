package br.com.clinics.catalog;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface ClinicRepository extends JpaRepository<Clinic, UUID> {

    Optional<Clinic> findByOwnerUserId(UUID ownerUserId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Clinic c WHERE c.id = :id")
    Optional<Clinic> findForUpdate(UUID id);

    @Query(value = """
            SELECT neighborhood AS name, count(*) AS "clinicsCount" FROM clinics
            WHERE neighborhood IS NOT NULL
            GROUP BY neighborhood ORDER BY unaccent(lower(neighborhood))""", nativeQuery = true)
    List<NeighborhoodRow> findNeighborhoods();

    interface NeighborhoodRow {

        String getName();

        long getClinicsCount();
    }
}
