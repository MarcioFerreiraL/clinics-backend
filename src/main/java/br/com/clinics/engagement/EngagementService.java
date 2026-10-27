package br.com.clinics.engagement;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.com.clinics.catalog.Category;
import br.com.clinics.common.ApiException;
import br.com.clinics.common.Page;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** O que o paciente faz com o catálogo: favoritos, economia acumulada e avaliações das clínicas. */
@Service
public class EngagementService {

    record FavoriteItem(UUID examTypeId, String name, String slug, String category, BigDecimal minPrice, int offersCount,
            Instant favoritedAt) {
    }

    record SavingsResponse(BigDecimal totalSavings, long contactsCount) {
    }

    record Review(UUID id, UUID clinicId, int rating, String comment, Instant createdAt, Instant updatedAt) {
    }

    record PublicReview(UUID id, int rating, String comment, String authorName, Instant createdAt) {
    }

    record Submitted(Review review, boolean created) {
    }

    private final JdbcClient jdbc;
    private final Clock clock;

    public EngagementService(JdbcClient jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    // ---- favoritos: adicionar ou remover de novo não tem efeito ----

    @Transactional
    public void addFavorite(UUID userId, UUID examTypeId) {
        if (!exists("exam_types", examTypeId)) {
            throw ApiException.notFound("EXAM_TYPE_NOT_FOUND", "Exame não encontrado.");
        }
        jdbc.sql("INSERT INTO favorites (user_id, exam_type_id) VALUES (?, ?) ON CONFLICT DO NOTHING")
                .params(userId, examTypeId).update();
    }

    @Transactional
    public void removeFavorite(UUID userId, UUID examTypeId) {
        jdbc.sql("DELETE FROM favorites WHERE user_id = ? AND exam_type_id = ?").params(userId, examTypeId).update();
    }

    /** Favoritos com o menor preço atual entre as clínicas. */
    @Transactional(readOnly = true)
    public Page<FavoriteItem> favorites(UUID userId, Integer page, Integer size) {
        int p = Page.page(page);
        int s = Page.size(size);
        List<FavoriteItem> items = jdbc.sql("""
                        SELECT t.id, t.name, t.slug, t.category, f.created_at,
                               MIN(e.price) FILTER (WHERE e.available) AS min_price,
                               COUNT(e.id) FILTER (WHERE e.available) AS offers_count
                        FROM favorites f
                        JOIN exam_types t ON t.id = f.exam_type_id
                        LEFT JOIN exams e ON e.exam_type_id = t.id
                        WHERE f.user_id = ?
                        GROUP BY t.id, t.name, t.slug, t.category, f.created_at
                        ORDER BY f.created_at DESC, t.name
                        LIMIT ? OFFSET ?""")
                .params(userId, s, p * s)
                .query((rs, row) -> new FavoriteItem(rs.getObject("id", UUID.class), rs.getString("name"),
                        rs.getString("slug"), Category.valueOf(rs.getString("category")).label(),
                        rs.getBigDecimal("min_price"), rs.getInt("offers_count"),
                        rs.getTimestamp("created_at").toInstant()))
                .list();
        long total = jdbc.sql("SELECT COUNT(*) FROM favorites WHERE user_id = ?").param(userId).query(Long.class).single();
        return Page.of(items, p, s, total);
    }

    /** Soma da economia congelada em cada contato pelo WhatsApp. */
    @Transactional(readOnly = true)
    public SavingsResponse savings(UUID userId) {
        return jdbc.sql("""
                        SELECT COALESCE(SUM(estimated_savings), 0) AS total, COUNT(*) AS contacts
                        FROM contact_events WHERE user_id = ?""")
                .param(userId)
                .query((rs, row) -> new SavingsResponse(rs.getBigDecimal("total").setScale(2), rs.getLong("contacts")))
                .single();
    }

    // ---- avaliações: uma por paciente e clínica; reavaliar substitui a anterior ----

    @Transactional
    public Submitted submitReview(UUID userId, UUID clinicId, int rating, String comment) {
        requireClinic(clinicId);
        if (rating < 1 || rating > 5) {
            throw ApiException.badRequest("INVALID_RATING", "A nota deve ser um número de 1 a 5.");
        }
        String text = comment == null || comment.isBlank() ? null : comment.strip();
        if (text != null && text.length() > 1000) {
            throw ApiException.badRequest("COMMENT_TOO_LONG", "O comentário deve ter no máximo 1000 caracteres.");
        }
        Instant now = clock.instant();
        Optional<Review> existing = findReview(userId, clinicId);
        Review review = existing
                .map(r -> new Review(r.id(), clinicId, rating, text, r.createdAt(), now))
                .orElseGet(() -> new Review(UUID.randomUUID(), clinicId, rating, text, now, now));
        jdbc.sql("""
                        INSERT INTO reviews (id, clinic_id, user_id, rating, comment, created_at, updated_at)
                        VALUES (:id, :clinicId, :userId, :rating, :comment, :createdAt, :updatedAt)
                        ON CONFLICT (user_id, clinic_id)
                        DO UPDATE SET rating = EXCLUDED.rating, comment = EXCLUDED.comment, updated_at = EXCLUDED.updated_at""")
                .param("id", review.id())
                .param("clinicId", clinicId)
                .param("userId", userId)
                .param("rating", rating)
                .param("comment", text, Types.VARCHAR)
                .param("createdAt", Timestamp.from(review.createdAt()))
                .param("updatedAt", Timestamp.from(now))
                .update();
        updateClinicRating(clinicId);
        return new Submitted(review, existing.isEmpty());
    }

    @Transactional(readOnly = true)
    public Review myReview(UUID userId, UUID clinicId) {
        requireClinic(clinicId);
        return findReview(userId, clinicId)
                .orElseThrow(() -> ApiException.notFound("REVIEW_NOT_FOUND", "Você ainda não avaliou esta clínica."));
    }

    /** Da mais recente para a mais antiga; o autor aparece só com o primeiro nome e a inicial do último (LGPD). */
    @Transactional(readOnly = true)
    public Page<PublicReview> reviews(UUID clinicId, Integer page, Integer size) {
        requireClinic(clinicId);
        int p = Page.page(page);
        int s = Page.size(size);
        List<PublicReview> items = jdbc.sql("""
                        SELECT r.*, u.full_name FROM reviews r JOIN users u ON u.id = r.user_id
                        WHERE r.clinic_id = ? ORDER BY r.created_at DESC, r.id LIMIT ? OFFSET ?""")
                .params(clinicId, s, p * s)
                .query((rs, row) -> new PublicReview(rs.getObject("id", UUID.class), rs.getInt("rating"),
                        rs.getString("comment"), abbreviate(rs.getString("full_name")),
                        rs.getTimestamp("created_at").toInstant()))
                .list();
        long total = jdbc.sql("SELECT COUNT(*) FROM reviews WHERE clinic_id = ?").param(clinicId).query(Long.class).single();
        return Page.of(items, p, s, total);
    }

    /** Nota da clínica = média com uma casa decimal, ou zero sem avaliações. Atualizada na mesma transação. */
    private void updateClinicRating(UUID clinicId) {
        jdbc.sql("""
                        UPDATE clinics SET
                            rating = COALESCE((SELECT ROUND(AVG(rating)::numeric, 1) FROM reviews WHERE clinic_id = :id), 0),
                            review_count = (SELECT COUNT(*) FROM reviews WHERE clinic_id = :id),
                            updated_at = now()
                        WHERE id = :id""")
                .param("id", clinicId)
                .update();
    }

    static String abbreviate(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "Paciente";
        }
        String[] parts = fullName.strip().split("\\s+");
        if (parts.length == 1) {
            return parts[0];
        }
        return parts[0] + " " + Character.toUpperCase(parts[parts.length - 1].charAt(0)) + ".";
    }

    private Optional<Review> findReview(UUID userId, UUID clinicId) {
        return jdbc.sql("SELECT * FROM reviews WHERE user_id = ? AND clinic_id = ?")
                .params(userId, clinicId)
                .query(EngagementService::mapReview)
                .optional();
    }

    private void requireClinic(UUID clinicId) {
        if (!exists("clinics", clinicId)) {
            throw ApiException.notFound("CLINIC_NOT_FOUND", "Clínica não encontrada.");
        }
    }

    private boolean exists(String table, UUID id) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM " + table + " WHERE id = ?)").param(id).query(Boolean.class).single();
    }

    private static Review mapReview(ResultSet rs, int row) throws SQLException {
        return new Review(rs.getObject("id", UUID.class), rs.getObject("clinic_id", UUID.class), rs.getInt("rating"),
                rs.getString("comment"), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }
}
