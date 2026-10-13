package br.com.clinics.catalog.infrastructure.persistence;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import br.com.clinics.catalog.application.port.out.ExamOfferRepository;
import br.com.clinics.catalog.domain.ExamCategory;
import br.com.clinics.catalog.domain.ExamOffer;
import br.com.clinics.catalog.domain.GeoPoint;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Consulta nativa: funções de janela calculam média e mínimo por tipo, e {@code unaccent} faz a busca sem acento.
 */
@Repository
class ExamOfferJdbcRepository implements ExamOfferRepository {

    private static final String SQL = """
            SELECT e.id AS exam_id, t.id AS exam_type_id, t.name AS exam_name, t.category,
                   c.id AS clinic_id, c.name AS clinic_name, c.neighborhood, c.latitude, c.longitude,
                   c.rating, c.is_verified, c.whatsapp_number, c.logo_url,
                   e.price, e.preparation, e.payment_methods,
                   AVG(e.price) OVER (PARTITION BY e.exam_type_id) AS average_price,
                   MIN(e.price) OVER (PARTITION BY e.exam_type_id) AS min_price
            FROM exams e
            JOIN exam_types t ON t.id = e.exam_type_id
            JOIN clinics c ON c.id = e.clinic_id
            WHERE e.available
              AND (:category IS NULL OR t.category = :category)
              AND (:pattern IS NULL OR unaccent(lower(t.name)) LIKE unaccent(lower(:pattern)) ESCAPE '\\')
            """;

    private final JdbcClient jdbc;

    ExamOfferJdbcRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<ExamOffer> findAvailable(String text, ExamCategory category) {
        return jdbc.sql(SQL)
                .param("category", category == null ? null : category.name(), java.sql.Types.VARCHAR)
                .param("pattern", text == null ? null : "%" + escapeLike(text) + "%", java.sql.Types.VARCHAR)
                .query(ExamOfferJdbcRepository::map)
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
                rs.getString("payment_methods"));
    }
}
