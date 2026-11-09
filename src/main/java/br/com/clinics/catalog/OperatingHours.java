package br.com.clinics.catalog;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Intervalo de funcionamento em um dia da semana (1 = segunda). Abre inclusive e fecha exclusive. */
@Entity
@Table(name = "clinic_operating_hours")
public class OperatingHours {

    @Id
    private UUID id;
    private short dayOfWeek;
    private LocalTime opensAt;
    private LocalTime closesAt;

    protected OperatingHours() {
    }

    OperatingHours(int dayOfWeek, LocalTime opensAt, LocalTime closesAt) {
        this.id = UUID.randomUUID();
        this.dayOfWeek = (short) dayOfWeek;
        this.opensAt = opensAt;
        this.closesAt = closesAt;
    }

    boolean covers(ZonedDateTime moment) {
        LocalTime time = moment.toLocalTime();
        return moment.getDayOfWeek() == DayOfWeek.of(dayOfWeek) && !time.isBefore(opensAt) && time.isBefore(closesAt);
    }

    int dayOfWeek() {
        return dayOfWeek;
    }

    LocalTime opensAt() {
        return opensAt;
    }

    LocalTime closesAt() {
        return closesAt;
    }
}
