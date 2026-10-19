package br.com.clinics.controller;

import java.util.List;
import java.util.UUID;

import br.com.clinics.dto.ClinicManagementDtos.DashboardResponse;
import br.com.clinics.dto.ClinicManagementDtos.ExamListResponse;
import br.com.clinics.dto.ClinicManagementDtos.ExamOfferRequest;
import br.com.clinics.dto.ClinicManagementDtos.ExamTypeResponse;
import br.com.clinics.dto.ClinicManagementDtos.ManagedExamResponse;
import br.com.clinics.service.ManageClinicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/clinic/me")
@PreAuthorize("hasRole('CLINIC_MANAGER')")
@Tag(name = "Gestão da clínica")
public class ClinicManagementController {

    private final ManageClinicService management;

    public ClinicManagementController(ManageClinicService management) {
        this.management = management;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Indicadores da clínica nos últimos 30 dias")
    DashboardResponse dashboard(@AuthenticationPrincipal Jwt jwt) {
        return management.dashboard(userId(jwt));
    }

    @GetMapping("/exam-catalog")
    @Operation(summary = "Catálogo de tipos de exame disponíveis para a clínica")
    List<ExamTypeResponse> catalog() {
        return management.catalog();
    }

    @GetMapping("/exams")
    @Operation(summary = "Lista as ofertas de exames da clínica, inclusive inativas")
    ExamListResponse exams(@AuthenticationPrincipal Jwt jwt) {
        return management.exams(userId(jwt));
    }

    @PostMapping("/exams")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra uma oferta de exame")
    ManagedExamResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ExamOfferRequest request) {
        UUID id = management.createExam(userId(jwt), request);
        return management.exam(userId(jwt), id);
    }

    @PutMapping("/exams/{id}")
    @Operation(summary = "Atualiza os dados de uma oferta de exame")
    ManagedExamResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @Valid @RequestBody ExamOfferRequest request) {
        management.updateExam(userId(jwt), id, request);
        return management.exam(userId(jwt), id);
    }

    @DeleteMapping("/exams/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desativa uma oferta sem remover seu histórico")
    void deactivate(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        management.deactivateExam(userId(jwt), id);
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
