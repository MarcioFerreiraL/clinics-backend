package br.com.clinics.repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import br.com.clinics.repository.TrendingExamRepository;
import br.com.clinics.model.ExamCategory;
import br.com.clinics.model.TrendingExam;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Exames em alta: cruza o catálogo com os contatos pelo WhatsApp registrados nos últimos dias. */
@Repository
public class TrendingExamRepository {

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

    public TrendingExamRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }
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
