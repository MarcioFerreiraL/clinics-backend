package br.com.clinics.repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ClinicManagementRepository {

    private final JdbcClient jdbc;

    public ClinicManagementRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<UUID> findClinicIdByOwner(UUID userId) {
        return jdbc.sql("SELECT id FROM clinics WHERE owner_user_id = :userId")
                .param("userId", userId)
                .query(UUID.class)
                .optional();
    }

    public DashboardData dashboard(UUID clinicId, Timestamp since) {
        return jdbc.sql("""
                        SELECT c.name,
                               (SELECT COUNT(*) FROM exams e
                                WHERE e.clinic_id = c.id AND e.available) AS active_exams,
                               (SELECT COUNT(*) FROM exam_view_events v
                                JOIN exams e ON e.id = v.exam_id
                                WHERE e.clinic_id = c.id AND v.created_at >= :since) AS exam_views,
                               (SELECT COUNT(*) FROM contact_events ce
                                JOIN exams e ON e.id = ce.exam_id
                                WHERE e.clinic_id = c.id AND ce.created_at >= :since) AS whatsapp_clicks
                        FROM clinics c
                        WHERE c.id = :clinicId""")
                .param("clinicId", clinicId)
                .param("since", since)
                .query((rs, row) -> new DashboardData(
                        rs.getString("name"),
                        rs.getLong("active_exams"),
                        rs.getLong("exam_views"),
                        rs.getLong("whatsapp_clicks")))
                .single();
    }

    public List<CatalogItem> catalog() {
        return jdbc.sql("""
                        SELECT id, name, category, description
                        FROM exam_types
                        ORDER BY unaccent(lower(name))""")
                .query((rs, row) -> new CatalogItem(
                        rs.getObject("id", UUID.class),
                        rs.getString("name"),
                        rs.getString("category"),
                        rs.getString("description")))
                .list();
    }

    public List<ManagedExam> findExams(UUID clinicId) {
        return jdbc.sql("""
                        SELECT e.id, t.id AS exam_type_id, t.name, t.category,
                               COALESCE(e.description, t.description) AS description,
                               e.price, e.preparation, e.payment_methods, e.available
                        FROM exams e
                        JOIN exam_types t ON t.id = e.exam_type_id
                        WHERE e.clinic_id = :clinicId
                        ORDER BY unaccent(lower(t.name))""")
                .param("clinicId", clinicId)
                .query((rs, row) -> new ManagedExam(
                        rs.getObject("id", UUID.class),
                        rs.getObject("exam_type_id", UUID.class),
                        rs.getString("name"),
                        rs.getString("category"),
                        rs.getString("description"),
                        rs.getBigDecimal("price"),
                        rs.getString("preparation"),
                        rs.getString("payment_methods"),
                        rs.getBoolean("available")))
                .list();
    }

    public UUID createExam(UUID clinicId, UUID examTypeId, BigDecimal price, String description,
            String preparation, String paymentMethods, boolean available, Timestamp now) throws DuplicateKeyException {
        return jdbc.sql("""
                        INSERT INTO exams (clinic_id, exam_type_id, price, description, preparation, payment_methods,
                                           available, created_at, updated_at)
                        VALUES (:clinicId, :examTypeId, :price, :description, :preparation, :paymentMethods,
                                :available, :now, :now)
                        RETURNING id""")
                .param("clinicId", clinicId)
                .param("examTypeId", examTypeId)
                .param("price", price)
                .param("description", description)
                .param("preparation", preparation)
                .param("paymentMethods", paymentMethods)
                .param("available", available)
                .param("now", now)
                .query(UUID.class)
                .single();
    }

    public boolean updateExam(UUID clinicId, UUID examId, UUID examTypeId, BigDecimal price, String description,
            String preparation, String paymentMethods, boolean available, Timestamp now) {
        int updated = jdbc.sql("""
                        UPDATE exams
                        SET exam_type_id = :examTypeId, price = :price, description = :description,
                            preparation = :preparation, payment_methods = :paymentMethods,
                            available = :available, updated_at = :now
                        WHERE id = :examId AND clinic_id = :clinicId""")
                .param("clinicId", clinicId)
                .param("examId", examId)
                .param("examTypeId", examTypeId)
                .param("price", price)
                .param("description", description)
                .param("preparation", preparation)
                .param("paymentMethods", paymentMethods)
                .param("available", available)
                .param("now", now)
                .update();
        return updated == 1;
    }

    public void recordPrice(UUID examId, BigDecimal price, Timestamp now) {
        jdbc.sql("INSERT INTO price_history (exam_id, price, recorded_at) VALUES (:examId, :price, :now)")
                .param("examId", examId)
                .param("price", price)
                .param("now", now)
                .update();
    }

    public record DashboardData(String clinicName, long activeExams, long examViews, long whatsappClicks) {}

    public record CatalogItem(UUID id, String name, String category, String description) {}

    public record ManagedExam(UUID id, UUID examTypeId, String name, String category, String description,
            BigDecimal price, String preparation, String paymentMethods, boolean available) {}
}
