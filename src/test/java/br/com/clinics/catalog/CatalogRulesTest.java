package br.com.clinics.catalog;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import br.com.clinics.catalog.CatalogService.Clinic;
import br.com.clinics.catalog.CatalogService.Hours;
import br.com.clinics.catalog.CatalogService.Offer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Regras do catálogo que dependem de limites exatos, difíceis de cobrir só pela API. */
class CatalogRulesTest {

    private static final ZoneId RECIFE = ZoneId.of("America/Recife");

    private static Clinic clinicOpenMondays7to17() {
        return new Clinic(UUID.randomUUID(), "Clínica", null, "Rua A", "Centro", "Surubim", "PE", null, null, null,
                "5581999999999", null, BigDecimal.ZERO, 0, 0, false,
                List.of(new Hours(1, LocalTime.of(7, 0), LocalTime.of(17, 0))));
    }

    private static Offer offer(String price, String average, String min, Double lat, Double lng) {
        return new Offer(UUID.randomUUID(), UUID.randomUUID(), "Hemograma", Category.SANGUE, null, UUID.randomUUID(),
                "Clínica", "Centro", lat, lng, BigDecimal.ZERO, false, "5581999999999", null, new BigDecimal(price),
                null, null, new BigDecimal(average), new BigDecimal(min));
    }

    @Test
    @DisplayName("RN22: abre inclusive e fecha exclusive, só no dia da semana do horário")
    void openNow() {
        Clinic clinic = clinicOpenMondays7to17();
        ZonedDateTime monday = ZonedDateTime.of(2026, 10, 26, 0, 0, 0, 0, RECIFE);
        assertThat(clinic.openAt(monday.withHour(6).withMinute(59))).isFalse();
        assertThat(clinic.openAt(monday.withHour(7))).isTrue();
        assertThat(clinic.openAt(monday.withHour(16).withMinute(59))).isTrue();
        assertThat(clinic.openAt(monday.withHour(17))).isFalse();
        assertThat(clinic.openAt(monday.plusDays(1).withHour(10))).isFalse();
    }

    @Test
    @DisplayName("RN15: economia é a diferença para a média, nunca negativa; melhor preço é o menor do tipo")
    void savingsAndBestPrice() {
        Offer cheap = offer("30.00", "45.50", "30.00", null, null);
        Offer expensive = offer("60.00", "45.50", "30.00", null, null);
        assertThat(cheap.savings()).isEqualByComparingTo("15.50");
        assertThat(cheap.bestPrice()).isTrue();
        assertThat(expensive.savings()).isEqualByComparingTo("0.00");
        assertThat(expensive.bestPrice()).isFalse();
    }

    @Test
    @DisplayName("RN19: distância Haversine com uma casa decimal, nula sem coordenadas")
    void distance() {
        Offer recife = offer("30", "30", "30", -8.0476, -34.8770);
        double[] surubim = {-7.8330, -35.7550};
        assertThat(recife.distanceKm(surubim)).isBetween(95.0, 105.0);
        assertThat(recife.distanceKm(null)).isNull();
        assertThat(offer("30", "30", "30", null, null).distanceKm(surubim)).isNull();
    }
}
