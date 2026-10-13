package br.com.clinics.engagement.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.com.clinics.engagement.domain.PublishedReview;
import br.com.clinics.engagement.domain.RatingStats;
import br.com.clinics.engagement.domain.Review;

public interface ReviewRepository {

    boolean clinicExists(UUID clinicId);

    Optional<Review> findByUserAndClinic(UUID userId, UUID clinicId);

    /** Insere ou substitui a avaliação do par (usuário, clínica). */
    void save(Review review);

    RatingStats statsFor(UUID clinicId);

    /** Grava a nota média e a contagem desnormalizadas na clínica (decisão D6 do MER). */
    void updateClinicRating(UUID clinicId, RatingStats stats);

    List<PublishedReview> findByClinic(UUID clinicId, int limit, int offset);

    long countByClinic(UUID clinicId);
}
