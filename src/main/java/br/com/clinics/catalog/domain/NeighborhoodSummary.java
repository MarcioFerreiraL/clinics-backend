package br.com.clinics.catalog.domain;

/** Bairro com ao menos uma clínica cadastrada, usado no seletor do app (RN38). */
public record NeighborhoodSummary(String name, int clinicsCount) {
}
