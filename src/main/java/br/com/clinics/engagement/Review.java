package br.com.clinics.engagement;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Avaliação de uma clínica: uma por paciente e clínica. */
@Entity
@Table(name = "reviews")
public class Review {

    @Id
    private UUID id;
    private UUID clinicId;
    private UUID userId;
    private short rating;
    private String comment;
    private Instant createdAt;
    private Instant updatedAt;

    protected Review() {
    }

    UUID id() {
        return id;
    }

    UUID clinicId() {
        return clinicId;
    }

    int rating() {
        return rating;
    }

    String comment() {
        return comment;
    }

    Instant createdAt() {
        return createdAt;
    }

    Instant updatedAt() {
        return updatedAt;
    }
}
