package br.com.clinics.clinicadmin;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import br.com.clinics.clinicadmin.ClinicAdminService.Dashboard;
import br.com.clinics.clinicadmin.ClinicAdminService.ExamData;
import br.com.clinics.clinicadmin.ClinicAdminService.ExamTypeItem;
import br.com.clinics.clinicadmin.ClinicAdminService.ManagedExam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
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

import static br.com.clinics.common.SecurityConfig.userId;

/** Acesso restrito ao papel CLINIC_MANAGER (ver SecurityConfig). */
@RestController
@RequestMapping("/api/v1/clinic/me")
@Tag(name = "Gestão da clínica")
@SecurityRequirement(name = "bearer-jwt")
public class ClinicAdminController {

    private final ClinicAdminService admin;

    public ClinicAdminController(ClinicAdminService admin) {
        this.admin = admin;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Indicadores da clínica nos últimos 30 dias")
    Dashboard dashboard(@AuthenticationPrincipal Jwt jwt) {
        return admin.dashboard(userId(jwt));
    }

    @GetMapping("/exam-catalog")
    @Operation(summary = "Tipos de exame que a clínica pode oferecer")
    List<ExamTypeItem> catalog() {
        return admin.catalog();
    }

    @GetMapping("/exams")
    @Operation(summary = "Ofertas da clínica, inclusive as inativas")
    ExamListResponse exams(@AuthenticationPrincipal Jwt jwt) {
        return new ExamListResponse(admin.exams(userId(jwt)));
    }

    @PostMapping("/exams")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra uma oferta de exame")
    ManagedExam create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ExamOfferRequest request) {
        return admin.createExam(userId(jwt), request.toData());
    }

    @PutMapping("/exams/{id}")
    @Operation(summary = "Atualiza uma oferta de exame")
    ManagedExam update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @Valid @RequestBody ExamOfferRequest request) {
        return admin.updateExam(userId(jwt), id, request.toData());
    }

    @DeleteMapping("/exams/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desativa uma oferta sem apagar o histórico")
    void deactivate(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        admin.deactivateExam(userId(jwt), id);
    }

    record ExamListResponse(List<ManagedExam> exams) {
    }

    record ExamOfferRequest(
            @NotNull(message = "Selecione um exame do catálogo.") UUID examTypeId,
            @NotNull(message = "Informe o preço.") @DecimalMin(value = "0.00", message = "O preço não pode ser negativo.")
                    @Digits(integer = 8, fraction = 2, message = "Informe um preço com até 2 casas decimais.") BigDecimal price,
            @Size(max = 2000, message = "A descrição deve ter até 2000 caracteres.") String description,
            @Size(max = 255, message = "O preparo deve ter até 255 caracteres.") String preparation,
            @Size(max = 255, message = "As formas de pagamento devem ter até 255 caracteres.") String paymentMethods,
            Boolean available) {

        ExamData toData() {
            return new ExamData(examTypeId, price, trim(description), trim(preparation), trim(paymentMethods),
                    available == null || available);
        }

        private static String trim(String value) {
            return value == null || value.isBlank() ? null : value.strip();
        }
    }
}
