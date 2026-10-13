package br.com.clinics.catalog.infrastructure.web;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import br.com.clinics.catalog.application.port.in.GetClinicProfileUseCase.ClinicProfile;
import br.com.clinics.catalog.application.port.in.GetExamDetailsUseCase.ExamDetails;
import br.com.clinics.catalog.domain.Clinic;
import br.com.clinics.catalog.domain.ExamOffer;
import br.com.clinics.catalog.domain.OperatingHours;
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

    record OperatingHoursResponse(int dayOfWeek, String opensAt, String closesAt) {

        static OperatingHoursResponse from(OperatingHours h) {
            return new OperatingHoursResponse(h.dayOfWeek().getValue(), h.opensAt().toString(), h.closesAt().toString());
        }
    }

    record ClinicDetailResponse(UUID id, String name, String address, String neighborhood, String phoneNumber,
            String whatsappNumber, String logoUrl, BigDecimal rating, int reviewCount, boolean isVerified,
            Double latitude, Double longitude, boolean openNow, List<OperatingHoursResponse> operatingHours) {

        static ClinicDetailResponse from(Clinic c, boolean openNow) {
            return new ClinicDetailResponse(c.id(), c.name(), c.address(), c.neighborhood(), c.phoneNumber(),
                    c.whatsappNumber(), c.logoUrl(), c.rating(), c.reviewCount(), c.verified(),
                    c.location() == null ? null : c.location().latitude(),
                    c.location() == null ? null : c.location().longitude(), openNow,
                    c.operatingHours().stream().map(OperatingHoursResponse::from).toList());
        }
    }

    record ExamDetailResponse(UUID id, ExamTypeDetail examType, BigDecimal price, BigDecimal averagePrice,
            BigDecimal savings, boolean bestPrice, String preparation, String paymentMethods, Double distanceKm,
            ClinicDetailResponse clinic) {

        static ExamDetailResponse from(ExamDetails d) {
            ExamOffer o = d.offer();
            return new ExamDetailResponse(o.examId(),
                    new ExamTypeDetail(o.examTypeId(), o.examName(), o.category().label(), o.examDescription()),
                    o.price(), o.averagePrice(), o.savings(), o.isBestPrice(), o.preparation(), o.paymentMethods(),
                    d.distanceKm(), ClinicDetailResponse.from(d.clinic(), d.clinicOpenNow()));
        }
    }

    record ExamTypeDetail(UUID id, String name, String category, String description) {
    }

    record ClinicExamResponse(UUID id, ExamTypeSummary examType, BigDecimal price, boolean bestPrice,
            String preparation, String paymentMethods) {

        static ClinicExamResponse from(ExamOffer o) {
            return new ClinicExamResponse(o.examId(), new ExamTypeSummary(o.examTypeId(), o.examName(), o.category().label()),
                    o.price(), o.isBestPrice(), o.preparation(), o.paymentMethods());
        }
    }

    record ClinicProfileResponse(UUID id, String name, String description, String address, String neighborhood,
            String city, String state, String phoneNumber, String whatsappNumber, String logoUrl, BigDecimal rating,
            int reviewCount, int patientCount, boolean isVerified, Double latitude, Double longitude, boolean openNow,
            List<OperatingHoursResponse> operatingHours, int examsCount, List<ClinicExamResponse> exams) {

        static ClinicProfileResponse from(ClinicProfile p) {
            Clinic c = p.clinic();
            return new ClinicProfileResponse(c.id(), c.name(), c.description(), c.address(), c.neighborhood(), c.city(),
                    c.state(), c.phoneNumber(), c.whatsappNumber(), c.logoUrl(), c.rating(), c.reviewCount(),
                    c.patientCount(), c.verified(),
                    c.location() == null ? null : c.location().latitude(),
                    c.location() == null ? null : c.location().longitude(), p.openNow(),
                    c.operatingHours().stream().map(OperatingHoursResponse::from).toList(), p.offers().size(),
                    p.offers().stream().map(ClinicExamResponse::from).toList());
        }
    }
}
