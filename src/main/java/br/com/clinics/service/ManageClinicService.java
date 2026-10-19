package br.com.clinics.service;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

import br.com.clinics.dto.ClinicManagementDtos.ExamOfferRequest;
import br.com.clinics.dto.ClinicManagementDtos.DashboardResponse;
import br.com.clinics.dto.ClinicManagementDtos.ExamListResponse;
import br.com.clinics.dto.ClinicManagementDtos.ExamTypeResponse;
import br.com.clinics.dto.ClinicManagementDtos.ManagedExamResponse;
import br.com.clinics.exception.ConflictException;
import br.com.clinics.exception.NotFoundException;
import br.com.clinics.repository.ClinicManagementRepository;
import br.com.clinics.repository.ClinicManagementRepository.DashboardData;
import br.com.clinics.repository.ClinicManagementRepository.ManagedExam;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ManageClinicService {

    private final ClinicManagementRepository clinics;
    private final Clock clock;

    public ManageClinicService(ClinicManagementRepository clinics, Clock clock) {
        this.clinics = clinics;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DashboardResponse dashboard(UUID userId) {
        UUID clinicId = clinicId(userId);
        Timestamp since = Timestamp.from(clock.instant().minus(java.time.Duration.ofDays(30)));
        DashboardData data = clinics.dashboard(clinicId, since);
        return new DashboardResponse(data.clinicName(), data.activeExams(), data.examViews(), data.whatsappClicks(), 30);
    }

    @Transactional(readOnly = true)
    public List<ExamTypeResponse> catalog() {
        return clinics.catalog().stream()
                .map(item -> new ExamTypeResponse(item.id(), item.name(), item.category(), item.description()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ExamListResponse exams(UUID userId) {
        return examList(clinics.findExams(clinicId(userId)));
    }

    @Transactional
    public UUID createExam(UUID userId, ExamOfferRequest request) {
        UUID clinicId = clinicId(userId);
        requireCatalogItem(request.examTypeId());
        try {
            Timestamp now = Timestamp.from(clock.instant());
            UUID id = clinics.createExam(clinicId, request.examTypeId(), request.price(),
                    optionalText(request.description()), optionalText(request.preparation()),
                    optionalText(request.paymentMethods()), request.shouldBeAvailable(), now);
            clinics.recordPrice(id, request.price(), now);
            return id;
        } catch (DuplicateKeyException ex) {
            throw duplicateOffer();
        }
    }

    @Transactional
    public void updateExam(UUID userId, UUID examId, ExamOfferRequest request) {
        UUID clinicId = clinicId(userId);
        requireCatalogItem(request.examTypeId());
        ManagedExam existing = clinics.findExams(clinicId).stream()
                .filter(item -> item.id().equals(examId))
                .findFirst()
                .orElseThrow(ManageClinicService::examNotFound);
        Timestamp now = Timestamp.from(clock.instant());
        try {
            if (!clinics.updateExam(clinicId, examId, request.examTypeId(), request.price(),
                    optionalText(request.description()),
                    optionalText(request.preparation()), optionalText(request.paymentMethods()),
                    request.shouldBeAvailable(), now)) {
                throw examNotFound();
            }
            if (existing.price().compareTo(request.price()) != 0) {
                clinics.recordPrice(examId, request.price(), now);
            }
        } catch (DuplicateKeyException ex) {
            throw duplicateOffer();
        }
    }

    @Transactional
    public void deactivateExam(UUID userId, UUID examId) {
        UUID clinicId = clinicId(userId);
        ManagedExam exam = clinics.findExams(clinicId).stream()
                .filter(item -> item.id().equals(examId))
                .findFirst()
                .orElseThrow(ManageClinicService::examNotFound);
        var request = new ExamOfferRequest(exam.examTypeId(), exam.price(), exam.description(), exam.preparation(),
                exam.paymentMethods(), false);
        updateExam(userId, examId, request);
    }

    private UUID clinicId(UUID userId) {
        return clinics.findClinicIdByOwner(userId)
                .orElseThrow(() -> new NotFoundException("CLINIC_NOT_FOUND", "Nenhuma clínica está vinculada a esta conta."));
    }

    private void requireCatalogItem(UUID examTypeId) {
        if (clinics.catalog().stream().noneMatch(item -> item.id().equals(examTypeId))) {
            throw new NotFoundException("EXAM_TYPE_NOT_FOUND", "O exame selecionado não existe no catálogo.");
        }
    }

    private static String optionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }

    private static ExamListResponse examList(List<ManagedExam> exams) {
        return new ExamListResponse(exams.stream().map(ManageClinicService::examResponse).toList());
    }

    private static ManagedExamResponse examResponse(ManagedExam exam) {
        return new ManagedExamResponse(exam.id(),
                new ExamTypeResponse(exam.examTypeId(), exam.name(), exam.category(), exam.description()),
                exam.price(), exam.description(), exam.preparation(), exam.paymentMethods(), exam.available());
    }

    @Transactional(readOnly = true)
    public ManagedExamResponse exam(UUID userId, UUID examId) {
        return clinics.findExams(clinicId(userId)).stream()
                .filter(item -> item.id().equals(examId))
                .findFirst()
                .map(ManageClinicService::examResponse)
                .orElseThrow(ManageClinicService::examNotFound);
    }

    private static ConflictException duplicateOffer() {
        return new ConflictException("EXAM_ALREADY_OFFERED", "Sua clínica já possui uma oferta deste exame.");
    }

    private static NotFoundException examNotFound() {
        return new NotFoundException("EXAM_NOT_FOUND", "Exame não encontrado nesta clínica.");
    }
}
