package br.com.clinics.engagement;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "favorites")
public class Favorite {

    @Embeddable
    public record Id(UUID userId, UUID examTypeId) implements Serializable {
    }

    @EmbeddedId
    private Id id;
    private Instant createdAt;

    protected Favorite() {
    }
}
