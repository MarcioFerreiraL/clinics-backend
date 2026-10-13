package br.com.clinics.catalog.infrastructure.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import br.com.clinics.catalog.application.port.out.TrendingExamRepository;
import br.com.clinics.catalog.domain.ExamCategory;
import br.com.clinics.catalog.domain.TrendingExam;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Modelo de leitura: cruza o catálogo com os eventos de contato registrados pelo módulo engagement. */
@Repository
class TrendingExamJdbcRepository implements TrendingExamRepository {

    private static final String SQL = """
            SELECT t.id, t.name, t.slug, t.category,
                   MIN(e.price) AS min_price,
                   COUNT(e.id) AS offers_count,
                   COALESCE(c.contacts, 0) AS recent_contacts
            FROM exam_types t
            JOIN exams e ON e.exam_type_id = t.id AND e.available
            LEFT JOIN (
                SELECT ex.exam_type_id, COUNT(*) AS contacts
                FROM contact_events ce
                JOIN exams ex ON ex.id = ce.exam_id
                WHERE ce.created_at >= :since
                GROUP BY ex.exam_type_id
            ) c ON c.exam_type_id = t.id
            GROUP BY t.id, t.name, t.slug, t.category, c.contacts
            ORDER BY recent_contacts DESC, AVG(e.price) ASC, t.name
            LIMIT :limit
            """;

    private final JdbcClient jdbc;

    TrendingExamJdbcRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<TrendingExam> findTrending(Instant since, int limit) {
        return jdbc.sql(SQL)
                .param("since", Timestamp.from(since))
                .param("limit", limit)
                .query((rs, row) -> new TrendingExam(
                        rs.getObject("id", UUID.class),
                        rs.getString("name"),
                        rs.getString("slug"),
                        ExamCategory.valueOf(rs.getString("category")),
                        rs.getBigDecimal("min_price"),
                        rs.getInt("offers_count"),
                        rs.getLong("recent_contacts")))
                .list();
    }
}
