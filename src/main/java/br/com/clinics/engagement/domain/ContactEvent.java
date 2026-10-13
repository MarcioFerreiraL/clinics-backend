package br.com.clinics.engagement.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Fato histórico: o preço e a economia ficam congelados no momento do contato (decisão D5 do MER). */
public record ContactEvent(UUID id, UUID examId, UUID userId, BigDecimal priceAtContact, BigDecimal estimatedSavings,
        Instant createdAt) {

    public static ContactEvent of(UUID examId, UUID userId, BigDecimal price, BigDecimal savings, Instant now) {
        return new ContactEvent(UUID.randomUUID(), examId, userId, price, savings.max(BigDecimal.ZERO), now);
    }
}
