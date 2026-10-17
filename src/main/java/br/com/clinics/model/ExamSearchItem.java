package br.com.clinics.model;

/** Oferta já posicionada em relação ao paciente (distância opcional). */
public record ExamSearchItem(ExamOffer offer, Double distanceKm) {
}
