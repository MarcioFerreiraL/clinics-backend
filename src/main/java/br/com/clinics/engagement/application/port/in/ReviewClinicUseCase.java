package br.com.clinics.engagement.application.port.in;

import java.util.UUID;

import br.com.clinics.engagement.domain.PublishedReview;
import br.com.clinics.engagement.domain.Review;
import br.com.clinics.shared.domain.PageResult;

/** UC10 · Avaliar clínica. */
public interface ReviewClinicUseCase {

    /** Cria a avaliação do paciente ou substitui a anterior (RN25) e recalcula a nota da clínica (RN27). */
    SubmitResult submit(UUID userId, UUID clinicId, int rating, String comment);

    PageResult<PublishedReview> list(UUID clinicId, Integer page, Integer size);

    Review getMine(UUID userId, UUID clinicId);

    record SubmitResult(Review review, boolean created) {
    }
}
