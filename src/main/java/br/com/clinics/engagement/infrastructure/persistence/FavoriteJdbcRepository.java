package br.com.clinics.engagement.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import br.com.clinics.catalog.domain.ExamCategory;
import br.com.clinics.engagement.application.port.out.FavoriteRepository;
import br.com.clinics.engagement.domain.FavoriteExam;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class FavoriteJdbcRepository implements FavoriteRepository {

    private static final String LIST_SQL = """
            SELECT t.id, t.name, t.slug, t.category, f.created_at,
                   MIN(e.price) FILTER (WHERE e.available) AS min_price,
                   COUNT(e.id) FILTER (WHERE e.available) AS offers_count
            FROM favorites f
            JOIN exam_types t ON t.id = f.exam_type_id
            LEFT JOIN exams e ON e.exam_type_id = t.id
            WHERE f.user_id = :userId
            GROUP BY t.id, t.name, t.slug, t.category, f.created_at
            ORDER BY f.created_at DESC, t.name
            LIMIT :limit OFFSET :offset
            """;

    private final JdbcClient jdbc;

    FavoriteJdbcRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean examTypeExists(UUID examTypeId) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM exam_types WHERE id = :id)")
                .param("id", examTypeId).query(Boolean.class).single();
    }

    @Override
    public void add(UUID userId, UUID examTypeId) {
        jdbc.sql("INSERT INTO favorites (user_id, exam_type_id) VALUES (:userId, :typeId) ON CONFLICT DO NOTHING")
                .param("userId", userId).param("typeId", examTypeId).update();
    }

    @Override
    public void remove(UUID userId, UUID examTypeId) {
        jdbc.sql("DELETE FROM favorites WHERE user_id = :userId AND exam_type_id = :typeId")
                .param("userId", userId).param("typeId", examTypeId).update();
    }

    @Override
    public List<FavoriteExam> findByUser(UUID userId, int limit, int offset) {
        return jdbc.sql(LIST_SQL)
                .param("userId", userId).param("limit", limit).param("offset", offset)
                .query((rs, row) -> new FavoriteExam(
                        rs.getObject("id", UUID.class),
                        rs.getString("name"),
                        rs.getString("slug"),
                        ExamCategory.valueOf(rs.getString("category")),
                        rs.getBigDecimal("min_price"),
                        rs.getInt("offers_count"),
                        rs.getTimestamp("created_at").toInstant()))
                .list();
    }

    @Override
    public long countByUser(UUID userId) {
        return jdbc.sql("SELECT COUNT(*) FROM favorites WHERE user_id = :userId")
                .param("userId", userId).query(Long.class).single();
    }
}
