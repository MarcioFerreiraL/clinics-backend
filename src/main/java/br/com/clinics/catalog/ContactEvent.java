package br.com.clinics.catalog;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Fato histórico: preço e economia ficam congelados no momento do contato. Visitante não tem {@code userId}. */
@Entity
@Table(name = "contact_events")
public class ContactEvent {

    @Id
    private UUID id;
    private UUID examId;
    private UUID userId;
    private BigDecimal priceAtContact;
    private BigDecimal estimatedSavings;
    private Instant createdAt;

    protected ContactEvent() {
    }

    ContactEvent(UUID examId, UUID userId, BigDecimal priceAtContact, BigDecimal estimatedSavings, Instant createdAt) {
        this.id = UUID.randomUUID();
        this.examId = examId;
        this.userId = userId;
        this.priceAtContact = priceAtContact;
        this.estimatedSavings = estimatedSavings;
        this.createdAt = createdAt;
    }

    UUID id() {
        return id;
    }
}
