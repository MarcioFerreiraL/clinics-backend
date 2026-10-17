package br.com.clinics.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** RN27: nota média da clínica com uma casa decimal, e zero enquanto não houver avaliações. */
public record RatingStats(long sum, long count) {

    public BigDecimal average() {
        if (count == 0) {
            return BigDecimal.ZERO.setScale(1);
        }
        return BigDecimal.valueOf(sum).divide(BigDecimal.valueOf(count), 1, RoundingMode.HALF_UP);
    }
}
