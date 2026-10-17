package br.com.clinics.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * Oferta: um tipo de exame vendido por uma clínica (RN09).
 * {@code averagePrice} e {@code minPrice} são calculados sobre todas as ofertas disponíveis do mesmo tipo (RN14).
 */
public record ExamOffer(
        UUID examId,
        UUID examTypeId,
        String examName,
        ExamCategory category,
        UUID clinicId,
        String clinicName,
        String neighborhood,
        GeoPoint clinicLocation,
        BigDecimal clinicRating,
        boolean clinicVerified,
        String whatsappNumber,
        String logoUrl,
        BigDecimal price,
        BigDecimal averagePrice,
        BigDecimal minPrice,
        String preparation,
        String paymentMethods,
        String examDescription) {

    public ExamOffer(UUID examId, UUID examTypeId, String examName, ExamCategory category, UUID clinicId,
            String clinicName, String neighborhood, GeoPoint clinicLocation, BigDecimal clinicRating,
            boolean clinicVerified, String whatsappNumber, String logoUrl, BigDecimal price, BigDecimal averagePrice,
            BigDecimal minPrice, String preparation, String paymentMethods) {
        this(examId, examTypeId, examName, category, clinicId, clinicName, neighborhood, clinicLocation, clinicRating,
                clinicVerified, whatsappNumber, logoUrl, price, averagePrice, minPrice, preparation, paymentMethods, null);
    }

    /** RN15: economia em relação à média local, nunca negativa. */
    public BigDecimal savings() {
        return averagePrice.subtract(price).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    /** RN15: selo "Melhor preço" para a(s) oferta(s) de menor preço do tipo. */
    public boolean isBestPrice() {
        return price.compareTo(minPrice) == 0;
    }

    /** Distância em km com uma casa decimal, ou {@code null} se faltar alguma das coordenadas (RN19). */
    public Double distanceKmFrom(GeoPoint origin) {
        if (origin == null || clinicLocation == null) {
            return null;
        }
        return BigDecimal.valueOf(origin.distanceKmTo(clinicLocation)).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
