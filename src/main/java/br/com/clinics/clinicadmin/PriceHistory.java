package br.com.clinics.clinicadmin;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "price_history")
public class PriceHistory {

    @Id
    private UUID id;
    private UUID examId;
    private BigDecimal price;
    private Instant recordedAt;

    protected PriceHistory() {
    }

    PriceHistory(UUID examId, BigDecimal price, Instant recordedAt) {
        this.id = UUID.randomUUID();
        this.examId = examId;
        this.price = price;
        this.recordedAt = recordedAt;
    }
}
