package br.com.clinics.engagement.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RatingStatsAndReviewerNameTest {

    @Test
    void averageHasOneDecimalRoundedHalfUp() {
        assertThat(new RatingStats(24, 5).average()).isEqualByComparingTo("4.8");
        assertThat(new RatingStats(14, 3).average()).isEqualByComparingTo("4.7");
        assertThat(new RatingStats(9, 2).average()).isEqualByComparingTo("4.5");
        assertThat(new RatingStats(5, 1).average()).isEqualByComparingTo("5.0");
    }

    @Test
    void averageIsZeroWithoutReviews() {
        assertThat(new RatingStats(0, 0).average()).isEqualByComparingTo("0.0");
    }

    @Test
    void abbreviatesToFirstNameAndLastInitial() {
        assertThat(ReviewerName.abbreviate("Maria de Souza Vasconcelos")).isEqualTo("Maria V.");
        assertThat(ReviewerName.abbreviate("  joão   pereira ")).isEqualTo("joão P.");
        assertThat(ReviewerName.abbreviate("Madonna")).isEqualTo("Madonna");
    }

    @Test
    void fallsBackWhenNameIsMissing() {
        assertThat(ReviewerName.abbreviate(null)).isEqualTo("Paciente");
        assertThat(ReviewerName.abbreviate("   ")).isEqualTo("Paciente");
    }
}
