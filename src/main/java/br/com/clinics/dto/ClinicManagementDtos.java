package br.com.clinics.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class ClinicManagementDtos {

    private ClinicManagementDtos() {}

    public record DashboardResponse(String clinicName, long activeExams, long examViews, long whatsappClicks,
            int periodDays) {}

    public record ExamTypeResponse(UUID id, String name, String category, String description) {}

    public record ManagedExamResponse(UUID id, ExamTypeResponse examType, BigDecimal price, String description,
            String preparation, String paymentMethods, boolean available) {}

    public record ExamOfferRequest(
            @NotNull(message = "Selecione um exame do catálogo.") UUID examTypeId,
            @NotNull(message = "Informe o preço.") @DecimalMin(value = "0.00", message = "O preço não pode ser negativo.")
                    @Digits(integer = 8, fraction = 2, message = "Informe um preço com até 2 casas decimais.") BigDecimal price,
            @Size(max = 2000, message = "A descrição deve ter até 2000 caracteres.") String description,
            @Size(max = 255, message = "O preparo deve ter até 255 caracteres.") String preparation,
            @Size(max = 255, message = "As formas de pagamento devem ter até 255 caracteres.") String paymentMethods,
            Boolean available) {
        public boolean shouldBeAvailable() {
            return available == null || available;
        }
    }

    public record ExamListResponse(List<ManagedExamResponse> exams) {}
}
