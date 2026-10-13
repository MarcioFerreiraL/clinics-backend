package br.com.clinics.engagement.application.port.in;

import java.math.BigDecimal;
import java.net.URI;
import java.util.UUID;

/** UC07 · Contatar a clínica por WhatsApp. O usuário é opcional: visitantes também geram contato. */
public interface RegisterContactUseCase {

    ContactResult register(UUID examId, UUID userId);

    record ContactResult(UUID contactId, URI whatsappUrl, BigDecimal estimatedSavings) {
    }
}
