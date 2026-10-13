package br.com.clinics.catalog.domain;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClinicOpeningTest {

    private static final ZoneId RECIFE = ZoneId.of("America/Recife");

    private final Clinic clinic = new Clinic(UUID.randomUUID(), "Clínica", null, "Rua A", "Centro", "Surubim", "PE", null,
            null, "5581999999999", null, BigDecimal.ONE, 0, 0, true,
            List.of(new OperatingHours(DayOfWeek.MONDAY, LocalTime.of(7, 0), LocalTime.of(12, 0)),
                    new OperatingHours(DayOfWeek.MONDAY, LocalTime.of(14, 0), LocalTime.of(18, 0)),
                    new OperatingHours(DayOfWeek.SATURDAY, LocalTime.of(7, 0), LocalTime.of(11, 30))));

    // 2026-10-12 é segunda-feira; 2026-10-17 é sábado; 2026-10-18 é domingo
    private static ZonedDateTime at(int day, int hour, int minute) {
        return LocalDateTime.of(2026, 10, day, hour, minute).atZone(RECIFE);
    }

    @Test
    void isOpenInsideAnInterval() {
        assertThat(clinic.isOpenAt(at(12, 9, 30))).isTrue();
        assertThat(clinic.isOpenAt(at(17, 8, 0))).isTrue();
    }

    @Test
    void isClosedDuringTheLunchBreak() {
        assertThat(clinic.isOpenAt(at(12, 13, 0))).isFalse();
    }

    @Test
    void opensInclusiveAndClosesExclusive() {
        assertThat(clinic.isOpenAt(at(12, 7, 0))).isTrue();
        assertThat(clinic.isOpenAt(at(12, 12, 0))).isFalse();
        assertThat(clinic.isOpenAt(at(12, 6, 59))).isFalse();
    }

    @Test
    void isClosedOnDaysWithoutHours() {
        assertThat(clinic.isOpenAt(at(18, 10, 0))).isFalse();
        assertThat(clinic.isOpenAt(at(13, 10, 0))).isFalse();
    }
}
