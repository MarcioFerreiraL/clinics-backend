package br.com.clinics.engagement.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.com.clinics.engagement.application.port.out.ReviewRepository;
import br.com.clinics.engagement.domain.PublishedReview;
import br.com.clinics.engagement.domain.RatingStats;
import br.com.clinics.engagement.domain.Review;
import br.com.clinics.engagement.domain.ReviewerName;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class ReviewJdbcRepository implements ReviewRepository {

    private final JdbcClient jdbc;

    ReviewJdbcRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean clinicExists(UUID clinicId) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM clinics WHERE id = :id)")
                .param("id", clinicId).query(Boolean.class).single();
    }

    @Override
    public Optional<Review> findByUserAndClinic(UUID userId, UUID clinicId) {
        return jdbc.sql("SELECT * FROM reviews WHERE user_id = :userId AND clinic_id = :clinicId")
                .param("userId", userId).param("clinicId", clinicId)
                .query((rs, row) -> map(rs))
                .optional();
    }

    @Override
    public void save(Review r) {
        jdbc.sql("""
                        INSERT INTO reviews (id, clinic_id, user_id, rating, comment, created_at, updated_at)
                        VALUES (:id, :clinicId, :userId, :rating, :comment, :createdAt, :updatedAt)
                        ON CONFLICT (user_id, clinic_id)
                        DO UPDATE SET rating = EXCLUDED.rating, comment = EXCLUDED.comment, updated_at = EXCLUDED.updated_at""")
                .param("id", r.id())
                .param("clinicId", r.clinicId())
                .param("userId", r.userId())
                .param("rating", r.rating())
                .param("comment", r.comment(), java.sql.Types.VARCHAR)
                .param("createdAt", Timestamp.from(r.createdAt()))
                .param("updatedAt", Timestamp.from(r.updatedAt()))
                .update();
    }

    @Override
    public RatingStats statsFor(UUID clinicId) {
        return jdbc.sql("SELECT COALESCE(SUM(rating), 0) AS total, COUNT(*) AS n FROM reviews WHERE clinic_id = :id")
                .param("id", clinicId)
                .query((rs, row) -> new RatingStats(rs.getLong("total"), rs.getLong("n")))
                .single();
    }

    @Override
    public void updateClinicRating(UUID clinicId, RatingStats stats) {
        jdbc.sql("UPDATE clinics SET rating = :rating, review_count = :count, updated_at = now() WHERE id = :id")
                .param("rating", stats.average())
                .param("count", stats.count())
                .param("id", clinicId)
                .update();
    }

    @Override
    public List<PublishedReview> findByClinic(UUID clinicId, int limit, int offset) {
        return jdbc.sql("""
                        SELECT r.*, u.full_name FROM reviews r JOIN users u ON u.id = r.user_id
                        WHERE r.clinic_id = :id ORDER BY r.created_at DESC, r.id LIMIT :limit OFFSET :offset""")
                .param("id", clinicId).param("limit", limit).param("offset", offset)
                .query((rs, row) -> new PublishedReview(map(rs), ReviewerName.abbreviate(rs.getString("full_name"))))
                .list();
    }

    @Override
    public long countByClinic(UUID clinicId) {
        return jdbc.sql("SELECT COUNT(*) FROM reviews WHERE clinic_id = :id")
                .param("id", clinicId).query(Long.class).single();
    }

    private static Review map(ResultSet rs) throws SQLException {
        return new Review(rs.getObject("id", UUID.class), rs.getObject("clinic_id", UUID.class),
                rs.getObject("user_id", UUID.class), rs.getInt("rating"), rs.getString("comment"),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
    }
}
