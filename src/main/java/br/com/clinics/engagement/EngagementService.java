package br.com.clinics.engagement;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import br.com.clinics.catalog.Category;
import br.com.clinics.catalog.Clinic;
import br.com.clinics.catalog.ClinicRepository;
import br.com.clinics.catalog.ContactEventRepository;
import br.com.clinics.catalog.ExamTypeRepository;
import br.com.clinics.common.ApiException;
import br.com.clinics.common.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** O que o paciente faz com o catálogo: favoritos, economia acumulada e avaliações das clínicas. */
@Service
public class EngagementService {

    record FavoriteItem(UUID examTypeId, String name, String slug, String category, BigDecimal minPrice, int offersCount,
            Instant favoritedAt) {
    }

    record SavingsResponse(BigDecimal totalSavings, long contactsCount) {
    }

    record MyReview(UUID id, UUID clinicId, int rating, String comment, Instant createdAt, Instant updatedAt) {

        static MyReview from(Review r) {
            return new MyReview(r.id(), r.clinicId(), r.rating(), r.comment(), r.createdAt(), r.updatedAt());
        }
    }

    record PublicReview(UUID id, int rating, String comment, String authorName, Instant createdAt) {
    }

    record Submitted(MyReview review, boolean created) {
    }

    private final FavoriteRepository favorites;
    private final ReviewRepository reviews;
    private final ContactEventRepository contacts;
    private final ExamTypeRepository examTypes;
    private final ClinicRepository clinics;
    private final Clock clock;

    public EngagementService(FavoriteRepository favorites, ReviewRepository reviews, ContactEventRepository contacts,
            ExamTypeRepository examTypes, ClinicRepository clinics, Clock clock) {
        this.favorites = favorites;
        this.reviews = reviews;
        this.contacts = contacts;
        this.examTypes = examTypes;
        this.clinics = clinics;
        this.clock = clock;
    }

    // ---- favoritos: adicionar ou remover de novo não tem efeito ----

    @Transactional
    public void addFavorite(UUID userId, UUID examTypeId) {
        if (!examTypes.existsById(examTypeId)) {
            throw ApiException.notFound("EXAM_TYPE_NOT_FOUND", "Exame não encontrado.");
        }
        favorites.addIfAbsent(userId, examTypeId, clock.instant());
    }

    @Transactional
    public void removeFavorite(UUID userId, UUID examTypeId) {
        favorites.remove(userId, examTypeId);
    }

    /** Favoritos com o menor preço atual entre as clínicas. */
    @Transactional(readOnly = true)
    public Page<FavoriteItem> favorites(UUID userId, Integer page, Integer size) {
        int p = Page.page(page);
        int s = Page.size(size);
        List<FavoriteItem> items = favorites.findRows(userId, PageRequest.of(p, s)).stream()
                .map(f -> new FavoriteItem(f.getExamTypeId(), f.getName(), f.getSlug(),
                        Category.valueOf(f.getCategory()).label(), f.getMinPrice(), (int) f.getOffersCount(),
                        f.getFavoritedAt()))
                .toList();
        return Page.of(items, p, s, favorites.countByIdUserId(userId));
    }

    /** Soma da economia congelada em cada contato pelo WhatsApp. */
    @Transactional(readOnly = true)
    public SavingsResponse savings(UUID userId) {
        var row = contacts.summarizeByUser(userId);
        return new SavingsResponse(row.getTotal().setScale(2, RoundingMode.HALF_UP), row.getContacts());
    }

    // ---- avaliações: uma por paciente e clínica; reavaliar substitui a anterior ----

    @Transactional
    public Submitted submitReview(UUID userId, UUID clinicId, int rating, String comment) {
        requireClinic(clinicId);
        if (rating < 1 || rating > 5) {
            throw ApiException.badRequest("INVALID_RATING", "A nota deve ser um número de 1 a 5.");
        }
        String text = comment == null || comment.isBlank() ? null : comment.strip();
        if (text != null && text.length() > 1000) {
            throw ApiException.badRequest("COMMENT_TOO_LONG", "O comentário deve ter no máximo 1000 caracteres.");
        }
        Instant now = clock.instant();
        boolean created = reviews.findByUserIdAndClinicId(userId, clinicId).isEmpty();
        reviews.upsert(UUID.randomUUID(), clinicId, userId, rating, text, now);
        updateClinicRating(clinicId, now);
        Review review = reviews.findByUserIdAndClinicId(userId, clinicId).orElseThrow();
        return new Submitted(MyReview.from(review), created);
    }

    @Transactional(readOnly = true)
    public MyReview myReview(UUID userId, UUID clinicId) {
        requireClinic(clinicId);
        return reviews.findByUserIdAndClinicId(userId, clinicId).map(MyReview::from)
                .orElseThrow(() -> ApiException.notFound("REVIEW_NOT_FOUND", "Você ainda não avaliou esta clínica."));
    }

    /** Da mais recente para a mais antiga; o autor aparece só com o primeiro nome e a inicial do último (LGPD). */
    @Transactional(readOnly = true)
    public Page<PublicReview> reviews(UUID clinicId, Integer page, Integer size) {
        requireClinic(clinicId);
        int p = Page.page(page);
        int s = Page.size(size);
        List<PublicReview> items = reviews.findRows(clinicId, PageRequest.of(p, s)).stream()
                .map(r -> new PublicReview(r.getId(), r.getRating(), r.getComment(), abbreviate(r.getFullName()),
                        r.getCreatedAt()))
                .toList();
        return Page.of(items, p, s, reviews.countByClinicId(clinicId));
    }

    /**
     * Nota da clínica = média com uma casa decimal, ou zero sem avaliações. Atualizada na mesma transação.
     * Trava a clínica antes de contar: quem avaliar ao mesmo tempo espera e já conta esta avaliação.
     */
    private void updateClinicRating(UUID clinicId, Instant now) {
        Clinic clinic = clinics.findForUpdate(clinicId).orElseThrow();
        var stats = reviews.stats(clinicId);
        BigDecimal average = stats.getReviews() == 0 ? BigDecimal.ZERO.setScale(1)
                : BigDecimal.valueOf(stats.getTotal()).divide(BigDecimal.valueOf(stats.getReviews()), 1, RoundingMode.HALF_UP);
        clinic.updateRating(average, (int) stats.getReviews(), now);
        clinics.save(clinic);
    }

    static String abbreviate(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "Paciente";
        }
        String[] parts = fullName.strip().split("\\s+");
        if (parts.length == 1) {
            return parts[0];
        }
        return parts[0] + " " + Character.toUpperCase(parts[parts.length - 1].charAt(0)) + ".";
    }

    private void requireClinic(UUID clinicId) {
        if (!clinics.existsById(clinicId)) {
            throw ApiException.notFound("CLINIC_NOT_FOUND", "Clínica não encontrada.");
        }
    }
}
