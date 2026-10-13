package br.com.clinics.catalog.domain;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZonedDateTime;

/** Intervalo de funcionamento em um dia da semana. Abre inclusive e fecha exclusive. */
public record OperatingHours(DayOfWeek dayOfWeek, LocalTime opensAt, LocalTime closesAt) {

    public boolean covers(ZonedDateTime moment) {
        LocalTime time = moment.toLocalTime();
        return moment.getDayOfWeek() == dayOfWeek && !time.isBefore(opensAt) && time.isBefore(closesAt);
    }
}
