package br.com.clinics.catalog.domain;

import java.math.BigDecimal;
import java.util.UUID;

/** Tipo de exame em destaque: "a partir de" o menor preço entre as clínicas (RN30). */
public record TrendingExam(UUID examTypeId, String name, String slug, ExamCategory category, BigDecimal minPrice,
        int offersCount, long recentContacts) {
}
