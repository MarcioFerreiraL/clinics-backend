package br.com.clinics.dto;

import java.math.BigDecimal;
import br.com.clinics.model.ExamSearchItem;
import br.com.clinics.dto.PageResult;

public record SearchExamsQuery(String text, String category, String sortBy, Double latitude, Double longitude,
        Integer page, Integer size, String neighborhood, BigDecimal minPrice, BigDecimal maxPrice,
        BigDecimal minRating, Boolean bestPriceOnly) {
}
