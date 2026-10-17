package br.com.clinics.model;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExamOfferTest {

    private static ExamOffer offer(String price, String average, String min, GeoPoint location) {
        return new ExamOffer(UUID.randomUUID(), UUID.randomUUID(), "Hemograma Completo", ExamCategory.SANGUE,
                UUID.randomUUID(), "Clínica", "Centro", location, new BigDecimal("4.8"), true, "5581999999999", null,
                new BigDecimal(price), new BigDecimal(average), new BigDecimal(min), null, null);
    }

    @Test
    void savingsIsTheGapToTheAverage() {
        assertThat(offer("90.00", "118.00", "90.00", null).savings()).isEqualByComparingTo("28.00");
    }

    @Test
    void savingsIsNeverNegative() {
        assertThat(offer("140.00", "118.00", "90.00", null).savings()).isEqualByComparingTo("0.00");
    }

    @Test
    void cheapestOfferGetsTheBestPriceBadge() {
        assertThat(offer("90.00", "118.00", "90.00", null).isBestPrice()).isTrue();
        assertThat(offer("110.00", "118.00", "90.00", null).isBestPrice()).isFalse();
    }

    @Test
    void tiedOffersAreAllBestPrice() {
        assertThat(offer("90.00", "100.00", "90.00", null).isBestPrice()).isTrue();
    }

    @Test
    void distanceIsRoundedToOneDecimal() {
        ExamOffer o = offer("10.00", "10.00", "10.00", new GeoPoint(-7.8412, -35.7601));
        assertThat(o.distanceKmFrom(new GeoPoint(-7.8333, -35.7528))).isEqualTo(1.2);
    }

    @Test
    void distanceIsNullWithoutOriginOrClinicLocation() {
        assertThat(offer("10.00", "10.00", "10.00", null).distanceKmFrom(new GeoPoint(0, 0))).isNull();
        assertThat(offer("10.00", "10.00", "10.00", new GeoPoint(0, 0)).distanceKmFrom(null)).isNull();
    }
}
