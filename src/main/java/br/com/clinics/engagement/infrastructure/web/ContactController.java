package br.com.clinics.engagement.infrastructure.web;

import java.math.BigDecimal;
import java.util.UUID;

import br.com.clinics.engagement.application.port.in.RegisterContactUseCase;
import br.com.clinics.engagement.application.port.in.RegisterContactUseCase.ContactResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/exams")
@Tag(name = "Contato")
class ContactController {

    private final RegisterContactUseCase contact;

    ContactController(RegisterContactUseCase contact) {
        this.contact = contact;
    }

    @PostMapping("/{id}/contacts")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra o contato e devolve o link do WhatsApp (UC07). Token é opcional.")
    ContactResponse register(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        UUID userId = jwt == null ? null : UUID.fromString(jwt.getSubject());
        return ContactResponse.from(contact.register(id, userId));
    }

    record ContactResponse(UUID contactId, String whatsappUrl, BigDecimal estimatedSavings) {

        static ContactResponse from(ContactResult r) {
            return new ContactResponse(r.contactId(), r.whatsappUrl().toString(), r.estimatedSavings());
        }
    }
}
