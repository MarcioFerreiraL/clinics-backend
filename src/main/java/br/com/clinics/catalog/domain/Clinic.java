package br.com.clinics.catalog.domain;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public record Clinic(
        UUID id,
        String name,
        String description,
        String address,
        String neighborhood,
        String city,
        String state,
        GeoPoint location,
        String phoneNumber,
        String whatsappNumber,
        String logoUrl,
        BigDecimal rating,
        int reviewCount,
        int patientCount,
        boolean verified,
        List<OperatingHours> operatingHours) {

    public Clinic {
        operatingHours = List.copyOf(operatingHours);
    }

    /** RN22: aberta se o momento cai em algum intervalo do dia da semana corrente (fuso do negócio). */
    public boolean isOpenAt(ZonedDateTime moment) {
        return operatingHours.stream().anyMatch(h -> h.covers(moment));
    }
}
