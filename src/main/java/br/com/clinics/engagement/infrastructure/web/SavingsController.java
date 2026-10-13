package br.com.clinics.engagement.infrastructure.web;

import java.math.BigDecimal;
import java.util.UUID;

import br.com.clinics.engagement.application.port.in.GetSavingsUseCase;
import br.com.clinics.engagement.domain.SavingsSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/savings")
@Tag(name = "Contato")
@SecurityRequirement(name = "bearer-jwt")
class SavingsController {

    private final GetSavingsUseCase savings;

    SavingsController(GetSavingsUseCase savings) {
        this.savings = savings;
    }

    @GetMapping
    @Operation(summary = "Total economizado pelo paciente e número de contatos (UC08)")
    SavingsResponse get(@AuthenticationPrincipal Jwt jwt) {
        return SavingsResponse.from(savings.getSavings(UUID.fromString(jwt.getSubject())));
    }

    record SavingsResponse(BigDecimal totalSavings, long contactsCount) {

        static SavingsResponse from(SavingsSummary s) {
            return new SavingsResponse(s.totalSavings(), s.contactsCount());
        }
    }
}
