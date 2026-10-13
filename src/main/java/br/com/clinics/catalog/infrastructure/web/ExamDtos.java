package br.com.clinics.catalog.infrastructure.web;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import br.com.clinics.catalog.domain.ExamOffer;
import br.com.clinics.catalog.domain.ExamSearchItem;
import br.com.clinics.shared.domain.PageResult;

final class ExamDtos {

    private ExamDtos() {
    }

    record ExamTypeSummary(UUID id, String name, String category) {
    }

    record ClinicSummary(UUID id, String name, String neighborhood, BigDecimal rating, boolean isVerified,
            String whatsappNumber, String logoUrl) {
    }

    record ExamOfferResponse(UUID id, ExamTypeSummary examType, ClinicSummary clinic, BigDecimal price,
            BigDecimal averagePrice, BigDecimal savings, boolean bestPrice, Double distanceKm, String preparation,
            String paymentMethods) {

        static ExamOfferResponse from(ExamSearchItem item) {
            ExamOffer o = item.offer();
            return new ExamOfferResponse(
                    o.examId(),
                    new ExamTypeSummary(o.examTypeId(), o.examName(), o.category().label()),
                    new ClinicSummary(o.clinicId(), o.clinicName(), o.neighborhood(), o.clinicRating(),
                            o.clinicVerified(), o.whatsappNumber(), o.logoUrl()),
                    o.price(), o.averagePrice(), o.savings(), o.isBestPrice(), item.distanceKm(),
                    o.preparation(), o.paymentMethods());
        }
    }

    record SearchResponse(long total, int page, int size, int totalPages, List<ExamOfferResponse> data) {

        static SearchResponse from(PageResult<ExamSearchItem> result) {
            return new SearchResponse(result.total(), result.page(), result.size(), result.totalPages(),
                    result.items().stream().map(ExamOfferResponse::from).toList());
        }
    }
}
