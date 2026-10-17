package br.com.clinics.model;

import java.util.Comparator;

import br.com.clinics.exception.ValidationException;

/** RN17: ordenações aceitas. Todo empate desempata pelo nome do exame e depois pelo preço. */
public enum ExamSort {
    PRICE,
    RATING,
    NAME,
    DISTANCE;

    public static ExamSort parse(String value) {
        if (value == null || value.isBlank()) {
            return PRICE;
        }
        try {
            return valueOf(value.strip().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("INVALID_SORT", "Ordenação inválida. Use: price, rating, name ou distance.");
        }
    }

    public Comparator<ExamSearchItem> comparator() {
        Comparator<ExamSearchItem> byName = Comparator.comparing(i -> i.offer().examName(), String.CASE_INSENSITIVE_ORDER);
        Comparator<ExamSearchItem> byPrice = Comparator.comparing(i -> i.offer().price());
        Comparator<ExamSearchItem> tieBreak = byName.thenComparing(byPrice);
        return switch (this) {
            case PRICE -> byPrice.thenComparing(byName);
            case RATING -> Comparator.<ExamSearchItem, java.math.BigDecimal>comparing(i -> i.offer().clinicRating())
                    .reversed().thenComparing(tieBreak);
            case NAME -> byName.thenComparing(byPrice);
            case DISTANCE -> Comparator.<ExamSearchItem, Double>comparing(ExamSearchItem::distanceKm,
                    Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(tieBreak);
        };
    }
}
