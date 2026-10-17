package br.com.clinics.repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.com.clinics.repository.ExamOfferRepository;
import br.com.clinics.model.ExamCategory;
import br.com.clinics.model.ExamOffer;
import br.com.clinics.model.GeoPoint;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Consulta nativa: funções de janela calculam média e mínimo por tipo, e {@code unaccent} faz a busca sem acento,
 * tanto pelo nome clínico quanto pelos nomes populares (apelidos) do exame (RN39).
 */
@Repository
public class ExamOfferRepository {

    /** Média e mínimo são calculados sobre todas as ofertas do tipo, antes de qualquer filtro externo (RN14). */
    private static final String BASE = """
            WITH offers AS (
                SELECT e.id AS exam_id, t.id AS exam_type_id, t.name AS exam_name, t.category,
                       t.description AS exam_description,
                       c.id AS clinic_id, c.name AS clinic_name, c.neighborhood, c.latitude, c.longitude,
                       c.rating, c.is_verified, c.whatsapp_number, c.logo_url,
                       e.price, e.preparation, e.payment_methods,
                       AVG(e.price) OVER (PARTITION BY e.exam_type_id) AS average_price,
                       MIN(e.price) OVER (PARTITION BY e.exam_type_id) AS min_price
                FROM exams e
                JOIN exam_types t ON t.id = e.exam_type_id
                JOIN clinics c ON c.id = e.clinic_id
                WHERE e.available
            )
            SELECT * FROM offers
            """;

    private static final String SEARCH_SQL = BASE + """
            WHERE (:category IS NULL OR category = :category)
              AND (:pattern IS NULL
                   OR unaccent(lower(exam_name)) LIKE unaccent(lower(:pattern)) ESCAPE '\\'
                   OR EXISTS (SELECT 1 FROM exam_type_aliases a
                              WHERE a.exam_type_id = offers.exam_type_id
                                AND (unaccent(lower(a.alias)) LIKE unaccent(lower(:pattern)) ESCAPE '\\'
                                     OR unaccent(lower(:text)) LIKE '%' || unaccent(lower(a.alias)) || '%')))
            """;

    private final JdbcClient jdbc;

    public ExamOfferRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }
    public List<ExamOffer> findAvailable(String text, ExamCategory category) {
        return jdbc.sql(SEARCH_SQL)
                .param("category", category == null ? null : category.name(), java.sql.Types.VARCHAR)
                .param("pattern", text == null ? null : "%" + escapeLike(text) + "%", java.sql.Types.VARCHAR)
                .param("text", text, java.sql.Types.VARCHAR)
                .query(ExamOfferRepository::map)
                .list();
    }
    public Optional<ExamOffer> findAvailableById(UUID examId) {
        return jdbc.sql(BASE + " WHERE exam_id = :id")
                .param("id", examId)
                .query(ExamOfferRepository::map)
                .optional();
    }
    public List<ExamOffer> findAvailableByClinic(UUID clinicId) {
        return jdbc.sql(BASE + " WHERE clinic_id = :id ORDER BY exam_name")
                .param("id", clinicId)
                .query(ExamOfferRepository::map)
                .list();
    }

    private static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static ExamOffer map(ResultSet rs, int row) throws SQLException {
        BigDecimal lat = rs.getBigDecimal("latitude");
        BigDecimal lng = rs.getBigDecimal("longitude");
        GeoPoint location = lat == null || lng == null ? null : new GeoPoint(lat.doubleValue(), lng.doubleValue());
        return new ExamOffer(
                rs.getObject("exam_id", UUID.class),
                rs.getObject("exam_type_id", UUID.class),
                rs.getString("exam_name"),
                ExamCategory.valueOf(rs.getString("category")),
                rs.getObject("clinic_id", UUID.class),
                rs.getString("clinic_name"),
                rs.getString("neighborhood"),
                location,
                rs.getBigDecimal("rating"),
                rs.getBoolean("is_verified"),
                rs.getString("whatsapp_number"),
                rs.getString("logo_url"),
                rs.getBigDecimal("price"),
                rs.getBigDecimal("average_price").setScale(2, RoundingMode.HALF_UP),
                rs.getBigDecimal("min_price"),
                rs.getString("preparation"),
                rs.getString("payment_methods"),
                rs.getString("exam_description"));
    }
}
