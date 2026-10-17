package br.com.clinics.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import br.com.clinics.model.ExamCategory;

/** Tipo de exame favoritado, com o menor preço atual entre as clínicas (RN23). */
public record FavoriteExam(UUID examTypeId, String name, String slug, ExamCategory category, BigDecimal minPrice,
        int offersCount, Instant favoritedAt) {
}
