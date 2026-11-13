package br.com.clinics.engagement;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface FavoriteRepository extends JpaRepository<Favorite, Favorite.Id> {

    /** Favoritos do usuário com o menor preço atual entre as clínicas, do mais recente para o mais antigo. */
    @Query("""
            SELECT t.id AS examTypeId, t.name AS name, t.slug AS slug, t.category AS category,
                   min(e.price) FILTER (WHERE e.available) AS minPrice,
                   count(e) FILTER (WHERE e.available) AS offersCount,
                   f.createdAt AS favoritedAt
            FROM Favorite f
            JOIN ExamType t ON t.id = f.id.examTypeId
            LEFT JOIN Exam e ON e.examTypeId = t.id
            WHERE f.id.userId = :userId
            GROUP BY t.id, t.name, t.slug, t.category, f.createdAt
            ORDER BY f.createdAt DESC, t.name""")
    List<FavoriteRow> findRows(UUID userId, Pageable pageable);

    long countByIdUserId(UUID userId);

    /** Atômico: dois toques seguidos no favorito não colidem na chave primária. */
    @Modifying
    @Query(value = """
            INSERT INTO favorites (user_id, exam_type_id, created_at) VALUES (:userId, :examTypeId, :now)
            ON CONFLICT DO NOTHING""", nativeQuery = true)
    void addIfAbsent(UUID userId, UUID examTypeId, Instant now);

    @Modifying
    @Query("DELETE FROM Favorite f WHERE f.id.userId = :userId AND f.id.examTypeId = :examTypeId")
    void remove(UUID userId, UUID examTypeId);

    interface FavoriteRow {

        UUID getExamTypeId();

        String getName();

        String getSlug();

        String getCategory();

        BigDecimal getMinPrice();

        long getOffersCount();

        Instant getFavoritedAt();
    }
}
