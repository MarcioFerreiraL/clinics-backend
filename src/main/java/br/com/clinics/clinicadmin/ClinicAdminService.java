package br.com.clinics.clinicadmin;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import br.com.clinics.common.ApiException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Painel do gestor: cada conta de gestor está ligada a uma clínica e só mexe nas ofertas dela. */
@Service
public class ClinicAdminService {

    record Dashboard(String clinicName, long activeExams, long examViews, long whatsappClicks, int periodDays) {
    }

    record ExamType(UUID id, String name, String category, String description) {
    }

    record ManagedExam(UUID id, ExamType examType, BigDecimal price, String description, String preparation,
            String paymentMethods, boolean available) {
    }

    /** Dados de uma oferta vindos do formulário, já sem espaços sobrando. */
    record ExamData(UUID examTypeId, BigDecimal price, String description, String preparation, String paymentMethods,
            boolean available) {
    }

    private final JdbcClient jdbc;
    private final Clock clock;

    public ClinicAdminService(JdbcClient jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    /** Indicadores dos últimos 30 dias. */
    @Transactional(readOnly = true)
    public Dashboard dashboard(UUID userId) {
        return jdbc.sql("""
                        SELECT c.name,
                               (SELECT COUNT(*) FROM exams e WHERE e.clinic_id = c.id AND e.available) AS active_exams,
                               (SELECT COUNT(*) FROM exam_view_events v JOIN exams e ON e.id = v.exam_id
                                WHERE e.clinic_id = c.id AND v.created_at >= :since) AS exam_views,
                               (SELECT COUNT(*) FROM contact_events ce JOIN exams e ON e.id = ce.exam_id
                                WHERE e.clinic_id = c.id AND ce.created_at >= :since) AS whatsapp_clicks
                        FROM clinics c WHERE c.id = :clinicId""")
                .param("clinicId", clinicId(userId))
                .param("since", Timestamp.from(clock.instant().minus(Duration.ofDays(30))))
                .query((rs, row) -> new Dashboard(rs.getString("name"), rs.getLong("active_exams"),
                        rs.getLong("exam_views"), rs.getLong("whatsapp_clicks"), 30))
                .single();
    }

    @Transactional(readOnly = true)
    public List<ExamType> catalog() {
        return jdbc.sql("SELECT id, name, category, description FROM exam_types ORDER BY unaccent(lower(name))")
                .query((rs, row) -> new ExamType(rs.getObject("id", UUID.class), rs.getString("name"),
                        rs.getString("category"), rs.getString("description")))
                .list();
    }

    /** Todas as ofertas da clínica, inclusive as desativadas. */
    @Transactional(readOnly = true)
    public List<ManagedExam> exams(UUID userId) {
        return jdbc.sql("""
                        SELECT e.id, t.id AS exam_type_id, t.name, t.category,
                               COALESCE(e.description, t.description) AS description,
                               e.price, e.preparation, e.payment_methods, e.available
                        FROM exams e JOIN exam_types t ON t.id = e.exam_type_id
                        WHERE e.clinic_id = ?
                        ORDER BY unaccent(lower(t.name))""")
                .param(clinicId(userId))
                .query((rs, row) -> {
                    String description = rs.getString("description");
                    return new ManagedExam(rs.getObject("id", UUID.class),
                            new ExamType(rs.getObject("exam_type_id", UUID.class), rs.getString("name"),
                                    rs.getString("category"), description),
                            rs.getBigDecimal("price"), description, rs.getString("preparation"),
                            rs.getString("payment_methods"), rs.getBoolean("available"));
                })
                .list();
    }

    @Transactional(readOnly = true)
    public ManagedExam exam(UUID userId, UUID examId) {
        return exams(userId).stream().filter(e -> e.id().equals(examId)).findFirst()
                .orElseThrow(ClinicAdminService::examNotFound);
    }

    @Transactional
    public ManagedExam createExam(UUID userId, ExamData data) {
        UUID clinicId = clinicId(userId);
        requireExamType(data.examTypeId());
        Timestamp now = Timestamp.from(clock.instant());
        try {
            UUID id = jdbc.sql("""
                            INSERT INTO exams (clinic_id, exam_type_id, price, description, preparation, payment_methods,
                                               available, created_at, updated_at)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                            RETURNING id""")
                    .params(clinicId, data.examTypeId(), data.price(), data.description(), data.preparation(),
                            data.paymentMethods(), data.available(), now, now)
                    .query(UUID.class)
                    .single();
            recordPrice(id, data.price(), now);
            return exam(userId, id);
        } catch (DuplicateKeyException e) {
            throw duplicateOffer();
        }
    }

    /** Atualiza a oferta; o histórico de preços só ganha uma linha quando o preço muda. */
    @Transactional
    public ManagedExam updateExam(UUID userId, UUID examId, ExamData data) {
        UUID clinicId = clinicId(userId);
        requireExamType(data.examTypeId());
        ManagedExam existing = exam(userId, examId);
        Timestamp now = Timestamp.from(clock.instant());
        try {
            jdbc.sql("""
                            UPDATE exams
                            SET exam_type_id = ?, price = ?, description = ?, preparation = ?, payment_methods = ?,
                                available = ?, updated_at = ?
                            WHERE id = ? AND clinic_id = ?""")
                    .params(data.examTypeId(), data.price(), data.description(), data.preparation(),
                            data.paymentMethods(), data.available(), now, examId, clinicId)
                    .update();
        } catch (DuplicateKeyException e) {
            throw duplicateOffer();
        }
        if (existing.price().compareTo(data.price()) != 0) {
            recordPrice(examId, data.price(), now);
        }
        return exam(userId, examId);
    }

    /** Desativa sem apagar, para não perder o histórico de contatos e visualizações. */
    @Transactional
    public void deactivateExam(UUID userId, UUID examId) {
        ManagedExam e = exam(userId, examId);
        updateExam(userId, examId, new ExamData(e.examType().id(), e.price(), e.description(), e.preparation(),
                e.paymentMethods(), false));
    }

    private void recordPrice(UUID examId, BigDecimal price, Timestamp now) {
        jdbc.sql("INSERT INTO price_history (exam_id, price, recorded_at) VALUES (?, ?, ?)")
                .params(examId, price, now).update();
    }

    private UUID clinicId(UUID userId) {
        return jdbc.sql("SELECT id FROM clinics WHERE owner_user_id = ?").param(userId).query(UUID.class).optional()
                .orElseThrow(() -> ApiException.notFound("CLINIC_NOT_FOUND", "Nenhuma clínica está vinculada a esta conta."));
    }

    private void requireExamType(UUID examTypeId) {
        boolean exists = jdbc.sql("SELECT EXISTS (SELECT 1 FROM exam_types WHERE id = ?)").param(examTypeId)
                .query(Boolean.class).single();
        if (!exists) {
            throw ApiException.notFound("EXAM_TYPE_NOT_FOUND", "O exame selecionado não existe no catálogo.");
        }
    }

    private static ApiException duplicateOffer() {
        return ApiException.conflict("EXAM_ALREADY_OFFERED", "Sua clínica já possui uma oferta deste exame.");
    }

    private static ApiException examNotFound() {
        return ApiException.notFound("EXAM_NOT_FOUND", "Exame não encontrado nesta clínica.");
    }
}
