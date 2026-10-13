package br.com.clinics.engagement.application;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

import br.com.clinics.engagement.application.port.in.ReviewClinicUseCase;
import br.com.clinics.engagement.application.port.out.ReviewRepository;
import br.com.clinics.engagement.domain.PublishedReview;
import br.com.clinics.engagement.domain.Review;
import br.com.clinics.shared.domain.NotFoundException;
import br.com.clinics.shared.domain.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ReviewClinicService implements ReviewClinicUseCase {

    private final ReviewRepository reviews;
    private final Clock clock;

    ReviewClinicService(ReviewRepository reviews, Clock clock) {
        this.reviews = reviews;
        this.clock = clock;
    }

    @Override
    @Transactional
    public SubmitResult submit(UUID userId, UUID clinicId, int rating, String comment) {
        requireClinic(clinicId);

        Optional<Review> existing = reviews.findByUserAndClinic(userId, clinicId);
        Review review;
        if (existing.isPresent()) {
            review = existing.get();
            review.edit(rating, comment, clock.instant());
        } else {
            review = Review.create(clinicId, userId, rating, comment, clock.instant());
        }
        reviews.save(review);

        // Mesma transação da gravação: a nota da clínica nunca fica defasada em relação às avaliações (RN27).
        reviews.updateClinicRating(clinicId, reviews.statsFor(clinicId));
        return new SubmitResult(review, existing.isEmpty());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<PublishedReview> list(UUID clinicId, Integer page, Integer size) {
        requireClinic(clinicId);
        int p = Paging.page(page);
        int s = Paging.size(size);
        return new PageResult<>(reviews.findByClinic(clinicId, s, p * s), p, s, reviews.countByClinic(clinicId));
    }

    @Override
    @Transactional(readOnly = true)
    public Review getMine(UUID userId, UUID clinicId) {
        requireClinic(clinicId);
        return reviews.findByUserAndClinic(userId, clinicId)
                .orElseThrow(() -> new NotFoundException("REVIEW_NOT_FOUND", "Você ainda não avaliou esta clínica."));
    }

    private void requireClinic(UUID clinicId) {
        if (!reviews.clinicExists(clinicId)) {
            throw new NotFoundException("CLINIC_NOT_FOUND", "Clínica não encontrada.");
        }
    }
}
