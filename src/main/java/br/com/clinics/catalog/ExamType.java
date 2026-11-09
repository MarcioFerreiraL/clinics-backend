package br.com.clinics.catalog;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Item do catálogo: o exame em si, independente da clínica que o vende. */
@Entity
@Table(name = "exam_types")
public class ExamType {

    @Id
    private UUID id;
    private String name;
    private String slug;
    private String category;
    private String description;

    protected ExamType() {
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String slug() {
        return slug;
    }

    public String category() {
        return category;
    }

    public String description() {
        return description;
    }
}
