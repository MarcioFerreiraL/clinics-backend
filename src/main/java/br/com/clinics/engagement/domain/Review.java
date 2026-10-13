package br.com.clinics.engagement.domain;

import java.time.Instant;
import java.util.UUID;

import br.com.clinics.shared.domain.ValidationException;

/** Avaliação de uma clínica por um paciente: nota de 1 a 5 e comentário opcional (RN25, RN26). */
public class Review {

    public static final int MAX_COMMENT_LENGTH = 1000;

    private final UUID id;
    private final UUID clinicId;
    private final UUID userId;
    private int rating;
    private String comment;
    private final Instant createdAt;
    private Instant updatedAt;

    public Review(UUID id, UUID clinicId, UUID userId, int rating, String comment, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.clinicId = clinicId;
        this.userId = userId;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Review create(UUID clinicId, UUID userId, int rating, String comment, Instant now) {
        Review review = new Review(UUID.randomUUID(), clinicId, userId, rating, null, now, now);
        review.edit(rating, comment, now);
        return review;
    }

    /** Reavaliar substitui a avaliação anterior (RN25). */
    public void edit(int newRating, String newComment, Instant now) {
        if (newRating < 1 || newRating > 5) {
            throw new ValidationException("INVALID_RATING", "A nota deve ser um número de 1 a 5.");
        }
        String normalized = newComment == null || newComment.isBlank() ? null : newComment.strip();
        if (normalized != null && normalized.length() > MAX_COMMENT_LENGTH) {
            throw new ValidationException("COMMENT_TOO_LONG", "O comentário deve ter no máximo 1000 caracteres.");
        }
        this.rating = newRating;
        this.comment = normalized;
        this.updatedAt = now;
    }

    public UUID id() {
        return id;
    }

    public UUID clinicId() {
        return clinicId;
    }

    public UUID userId() {
        return userId;
    }

    public int rating() {
        return rating;
    }

    public String comment() {
        return comment;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
