package br.com.clinics.engagement.domain;

import java.time.Instant;
import java.util.UUID;

import br.com.clinics.shared.domain.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReviewTest {

    private static final Instant T0 = Instant.parse("2026-10-12T10:00:00Z");
    private static final Instant T1 = Instant.parse("2026-10-13T10:00:00Z");

    @ParameterizedTest
    @ValueSource(ints = {1, 3, 5})
    void acceptsRatingsFromOneToFive(int rating) {
        assertThat(Review.create(UUID.randomUUID(), UUID.randomUUID(), rating, "ok", T0).rating()).isEqualTo(rating);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 6, 100})
    void rejectsRatingsOutsideTheRange(int rating) {
        assertThatThrownBy(() -> Review.create(UUID.randomUUID(), UUID.randomUUID(), rating, null, T0))
                .isInstanceOf(ValidationException.class).hasFieldOrPropertyWithValue("code", "INVALID_RATING");
    }

    @Test
    void blankCommentBecomesNullAndCommentIsTrimmed() {
        assertThat(Review.create(UUID.randomUUID(), UUID.randomUUID(), 4, "   ", T0).comment()).isNull();
        assertThat(Review.create(UUID.randomUUID(), UUID.randomUUID(), 4, "  Bom  ", T0).comment()).isEqualTo("Bom");
    }

    @Test
    void commentLimitIsOneThousandCharacters() {
        assertThat(Review.create(UUID.randomUUID(), UUID.randomUUID(), 4, "a".repeat(1000), T0).comment()).hasSize(1000);
        assertThatThrownBy(() -> Review.create(UUID.randomUUID(), UUID.randomUUID(), 4, "a".repeat(1001), T0))
                .hasFieldOrPropertyWithValue("code", "COMMENT_TOO_LONG");
    }

    @Test
    void editingReplacesRatingAndCommentButKeepsIdentityAndCreationDate() {
        Review review = Review.create(UUID.randomUUID(), UUID.randomUUID(), 5, "Ótimo", T0);
        UUID id = review.id();

        review.edit(2, "Piorou", T1);

        assertThat(review.id()).isEqualTo(id);
        assertThat(review.rating()).isEqualTo(2);
        assertThat(review.comment()).isEqualTo("Piorou");
        assertThat(review.createdAt()).isEqualTo(T0);
        assertThat(review.updatedAt()).isEqualTo(T1);
    }

    @Test
    void failedEditLeavesTheReviewUntouched() {
        Review review = Review.create(UUID.randomUUID(), UUID.randomUUID(), 5, "Ótimo", T0);
        assertThatThrownBy(() -> review.edit(9, "x", T1)).isInstanceOf(ValidationException.class);
        assertThat(review.rating()).isEqualTo(5);
        assertThat(review.comment()).isEqualTo("Ótimo");
    }
}
