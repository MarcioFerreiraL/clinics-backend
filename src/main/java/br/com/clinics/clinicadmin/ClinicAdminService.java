package br.com.clinics.clinicadmin;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import br.com.clinics.catalog.Clinic;
import br.com.clinics.catalog.ClinicRepository;
import br.com.clinics.catalog.ContactEventRepository;
import br.com.clinics.catalog.Exam;
import br.com.clinics.catalog.ExamRepository;
import br.com.clinics.catalog.ExamTypeRepository;
import br.com.clinics.catalog.ExamViewEventRepository;
import br.com.clinics.common.ApiException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Painel do gestor: cada conta de gestor está ligada a uma clínica e só mexe nas ofertas dela. */
@Service
public class ClinicAdminService {

    record Dashboard(String clinicName, long activeExams, long examViews, long whatsappClicks, int periodDays) {
    }

    record ExamTypeItem(UUID id, String name, String category, String description) {
    }

    record ManagedExam(UUID id, ExamTypeItem examType, BigDecimal price, String description, String preparation,
            String paymentMethods, boolean available) {
    }

    /** Dados de uma oferta vindos do formulário, já sem espaços sobrando. */
    record ExamData(UUID examTypeId, BigDecimal price, String description, String preparation, String paymentMethods,
            boolean available) {
    }

    private final ClinicRepository clinics;
    private final ExamRepository exams;
    private final ExamTypeRepository examTypes;
    private final PriceHistoryRepository priceHistory;
    private final ContactEventRepository contacts;
    private final ExamViewEventRepository views;
    private final Clock clock;

    public ClinicAdminService(ClinicRepository clinics, ExamRepository exams, ExamTypeRepository examTypes,
            PriceHistoryRepository priceHistory, ContactEventRepository contacts, ExamViewEventRepository views,
            Clock clock) {
        this.clinics = clinics;
        this.exams = exams;
        this.examTypes = examTypes;
        this.priceHistory = priceHistory;
        this.contacts = contacts;
        this.views = views;
        this.clock = clock;
    }

    /** Indicadores dos últimos 30 dias. */
    @Transactional(readOnly = true)
    public Dashboard dashboard(UUID userId) {
        Clinic clinic = clinic(userId);
        Instant since = clock.instant().minus(Duration.ofDays(30));
        return new Dashboard(clinic.name(), exams.countByClinicIdAndAvailableTrue(clinic.id()),
                views.countByClinicSince(clinic.id(), since), contacts.countByClinicSince(clinic.id(), since), 30);
    }

    @Transactional(readOnly = true)
    public List<ExamTypeItem> catalog() {
        return examTypes.findAllByName().stream()
                .map(t -> new ExamTypeItem(t.id(), t.name(), t.category(), t.description()))
                .toList();
    }

    /** Todas as ofertas da clínica, inclusive as desativadas. */
    @Transactional(readOnly = true)
    public List<ManagedExam> exams(UUID userId) {
        return exams.findManagedExams(clinic(userId).id()).stream()
                .map(e -> new ManagedExam(e.getId(),
                        new ExamTypeItem(e.getExamTypeId(), e.getName(), e.getCategory(), e.getDescription()),
                        e.getPrice(), e.getDescription(), e.getPreparation(), e.getPaymentMethods(), e.getAvailable()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ManagedExam exam(UUID userId, UUID examId) {
        return exams(userId).stream().filter(e -> e.id().equals(examId)).findFirst()
                .orElseThrow(ClinicAdminService::examNotFound);
    }

    @Transactional
    public ManagedExam createExam(UUID userId, ExamData data) {
        UUID clinicId = clinic(userId).id();
        requireExamType(data.examTypeId());
        Instant now = clock.instant();
        Exam exam = new Exam(clinicId, now);
        save(exam, data, now);
        priceHistory.save(new PriceHistory(exam.id(), data.price(), now));
        return exam(userId, exam.id());
    }

    /** Atualiza a oferta; o histórico de preços só ganha uma linha quando o preço muda. */
    @Transactional
    public ManagedExam updateExam(UUID userId, UUID examId, ExamData data) {
        UUID clinicId = clinic(userId).id();
        requireExamType(data.examTypeId());
        Exam exam = exams.findByIdAndClinicId(examId, clinicId).orElseThrow(ClinicAdminService::examNotFound);
        boolean priceChanged = exam.price().compareTo(data.price()) != 0;
        Instant now = clock.instant();
        save(exam, data, now);
        if (priceChanged) {
            priceHistory.save(new PriceHistory(examId, data.price(), now));
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

    /** Grava já, para a oferta repetida na mesma clínica (UNIQUE no banco) virar 409 aqui. */
    private void save(Exam exam, ExamData data, Instant now) {
        exam.update(data.examTypeId(), data.price(), data.description(), data.preparation(), data.paymentMethods(),
                data.available(), now);
        try {
            exams.saveAndFlush(exam);
        } catch (DataIntegrityViolationException e) {
            throw duplicateOffer();
        }
    }

    private Clinic clinic(UUID userId) {
        return clinics.findByOwnerUserId(userId)
                .orElseThrow(() -> ApiException.notFound("CLINIC_NOT_FOUND", "Nenhuma clínica está vinculada a esta conta."));
    }

    private void requireExamType(UUID examTypeId) {
        if (!examTypes.existsById(examTypeId)) {
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
