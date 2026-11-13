package br.com.clinics.engagement;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    Optional<Review> findByUserIdAndClinicId(UUID userId, UUID clinicId);

    long countByClinicId(UUID clinicId);

    /** Atômico: dois envios simultâneos do mesmo paciente não colidem no UNIQUE (usuário, clínica). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT INTO reviews (id, clinic_id, user_id, rating, comment, created_at, updated_at)
            VALUES (:id, :clinicId, :userId, :rating, CAST(:comment AS varchar), :now, :now)
            ON CONFLICT (user_id, clinic_id)
            DO UPDATE SET rating = EXCLUDED.rating, comment = EXCLUDED.comment, updated_at = EXCLUDED.updated_at""",
            nativeQuery = true)
    void upsert(UUID id, UUID clinicId, UUID userId, int rating, String comment, Instant now);

    @Query("""
            SELECT r.id AS id, r.rating AS rating, r.comment AS comment, u.fullName AS fullName, r.createdAt AS createdAt
            FROM Review r JOIN User u ON u.id = r.userId
            WHERE r.clinicId = :clinicId
            ORDER BY r.createdAt DESC, r.id""")
    List<ReviewRow> findRows(UUID clinicId, Pageable pageable);

    @Query("SELECT coalesce(sum(r.rating), 0) AS total, count(r) AS reviews FROM Review r WHERE r.clinicId = :clinicId")
    StatsRow stats(UUID clinicId);

    interface ReviewRow {

        UUID getId();

        int getRating();

        String getComment();

        String getFullName();

        Instant getCreatedAt();
    }

    interface StatsRow {

        long getTotal();

        long getReviews();
    }
}
