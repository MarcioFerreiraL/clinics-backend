package br.com.clinics.access;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Permissão no formato recurso.ação, por exemplo {@code exame.editar}. */
@Entity
@Table(name = "permissions")
public class Permission {

    @Id
    private UUID id;
    private String resource;
    private String action;
    private String label;

    protected Permission() {
    }

    public UUID id() {
        return id;
    }

    public String key() {
        return resource + "." + action;
    }

    public String label() {
        return label;
    }
}
