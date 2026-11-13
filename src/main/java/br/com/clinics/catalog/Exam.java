package br.com.clinics.catalog;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Oferta: um tipo de exame vendido por uma clínica, com preço próprio. */
@Entity
@Table(name = "exams")
public class Exam {

    @Id
    private UUID id;
    @Column(updatable = false)
    private UUID clinicId;
    private UUID examTypeId;
    private BigDecimal price;
    private String description;
    private String preparation;
    private String paymentMethods;
    private boolean available;
    @Column(updatable = false)
    private Instant createdAt;
    private Instant updatedAt;

    protected Exam() {
    }

    public Exam(UUID clinicId, Instant now) {
        this.id = UUID.randomUUID();
        this.clinicId = clinicId;
        this.createdAt = now;
    }

    public void update(UUID examTypeId, BigDecimal price, String description, String preparation,
            String paymentMethods, boolean available, Instant now) {
        this.examTypeId = examTypeId;
        this.price = price;
        this.description = description;
        this.preparation = preparation;
        this.paymentMethods = paymentMethods;
        this.available = available;
        this.updatedAt = now;
    }

    public UUID id() {
        return id;
    }

    public BigDecimal price() {
        return price;
    }
}
