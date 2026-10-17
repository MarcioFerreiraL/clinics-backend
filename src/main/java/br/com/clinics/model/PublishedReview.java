package br.com.clinics.model;

/** Avaliação como aparece ao público, com o nome do autor já abreviado. */
public record PublishedReview(Review review, String authorName) {
}
