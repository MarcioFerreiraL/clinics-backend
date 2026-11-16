package br.com.clinics.access;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Cargo. {@code visibilityScope}: "all" enxerga todas as clínicas; "own_clinic" só a da lotação. */
@Entity
@Table(name = "positions")
public class Position {

    @Id
    private UUID id;
    private String name;
    private String visibilityScope;

    protected Position() {
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String visibilityScope() {
        return visibilityScope;
    }
}
