package br.com.clinics.catalog;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import br.com.clinics.catalog.CatalogService.Offer;
import br.com.clinics.common.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static br.com.clinics.common.SecurityConfig.userId;

/** Rotas públicas do catálogo: busca, detalhes, contato, exames em alta, clínicas e bairros. */
@RestController
@Tag(name = "Catálogo")
public class CatalogController {

    private final CatalogService catalog;

    public CatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/api/v1/exams/search")
    @Operation(summary = "Busca e compara ofertas de exames")
    Page<SearchItemResponse> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(name = "sort_by", required = false) String sortBy,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String neighborhood,
            @RequestParam(name = "min_price", required = false) BigDecimal minPrice,
            @RequestParam(name = "max_price", required = false) BigDecimal maxPrice,
            @RequestParam(name = "min_rating", required = false) BigDecimal minRating,
            @RequestParam(name = "best_price_only", required = false) Boolean bestPriceOnly) {
        return catalog.search(query, category, sortBy, lat, lng, page, size, neighborhood, minPrice, maxPrice,
                minRating, bestPriceOnly).map(SearchItemResponse::from);
    }

    @GetMapping("/api/v1/exams/{id}")
    @Operation(summary = "Detalhes de uma oferta de exame")
    ExamDetailResponse details(@PathVariable UUID id, @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng) {
        double[] origin = catalog.originFor(lat, lng);
        Offer offer = catalog.offer(id);
        Clinic clinic = catalog.clinic(offer.clinicId());
        catalog.recordView(id);
        return new ExamDetailResponse(offer.examId(),
                new ExamTypeDetail(offer.examTypeId(), offer.examName(), offer.category().label(), offer.examDescription()),
                offer.price(), offer.averagePrice(), offer.savings(), offer.bestPrice(), offer.preparation(),
                offer.paymentMethods(), offer.distanceKm(origin), ClinicDetailResponse.from(clinic, catalog.isOpenNow(clinic)));
    }

    @PostMapping("/api/v1/exams/{id}/contacts")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra o contato e devolve o link do WhatsApp (token opcional)")
    CatalogService.Contact contact(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return catalog.contact(id, jwt == null ? null : userId(jwt));
    }

    @GetMapping("/api/v1/exam-types/trending")
    @Operation(summary = "Exames mais contatados nos últimos 7 dias")
    List<TrendingResponse> trending() {
        return catalog.trending().stream()
                .map(t -> new TrendingResponse(t.id(), t.name(), t.slug(), t.category().label(), t.minPrice(),
                        t.offersCount(), t.recentContacts()))
                .toList();
    }

    @GetMapping("/api/v1/clinics/{id}")
    @Operation(summary = "Perfil público da clínica, horários e exames")
    ClinicProfileResponse clinic(@PathVariable UUID id) {
        Clinic c = catalog.clinic(id);
        List<ClinicExamResponse> exams = catalog.offersOfClinic(id).stream()
                .map(o -> new ClinicExamResponse(o.examId(), ExamTypeSummary.from(o), o.price(), o.bestPrice(),
                        o.preparation(), o.paymentMethods()))
                .toList();
        return new ClinicProfileResponse(c.id(), c.name(), c.description(), c.address(), c.neighborhood(), c.city(),
                c.state(), c.phoneNumber(), c.whatsappNumber(), c.logoUrl(), c.rating(), c.reviewCount(),
                c.patientCount(), c.verified(), c.latitude(), c.longitude(), catalog.isOpenNow(c), hours(c),
                exams.size(), exams);
    }

    @GetMapping("/api/v1/neighborhoods")
    @Operation(summary = "Bairros com clínicas cadastradas")
    List<CatalogService.Neighborhood> neighborhoods() {
        return catalog.neighborhoods();
    }

    private static List<OperatingHoursResponse> hours(Clinic c) {
        return c.hours().stream()
                .map(h -> new OperatingHoursResponse(h.dayOfWeek(), h.opensAt().toString(), h.closesAt().toString()))
                .toList();
    }

    record ExamTypeSummary(UUID id, String name, String category) {

        static ExamTypeSummary from(Offer o) {
            return new ExamTypeSummary(o.examTypeId(), o.examName(), o.category().label());
        }
    }

    record ClinicSummary(UUID id, String name, String neighborhood, BigDecimal rating, boolean isVerified,
            String whatsappNumber, String logoUrl) {
    }

    record SearchItemResponse(UUID id, ExamTypeSummary examType, ClinicSummary clinic, BigDecimal price,
            BigDecimal averagePrice, BigDecimal savings, boolean bestPrice, Double distanceKm, String preparation,
            String paymentMethods) {

        static SearchItemResponse from(CatalogService.SearchItem item) {
            Offer o = item.offer();
            return new SearchItemResponse(o.examId(), ExamTypeSummary.from(o),
                    new ClinicSummary(o.clinicId(), o.clinicName(), o.neighborhood(), o.clinicRating(),
                            o.clinicVerified(), o.whatsappNumber(), o.logoUrl()),
                    o.price(), o.averagePrice(), o.savings(), o.bestPrice(), item.distanceKm(), o.preparation(),
                    o.paymentMethods());
        }
    }

    record OperatingHoursResponse(int dayOfWeek, String opensAt, String closesAt) {
    }

    record ClinicDetailResponse(UUID id, String name, String address, String neighborhood, String phoneNumber,
            String whatsappNumber, String logoUrl, BigDecimal rating, int reviewCount, boolean isVerified,
            Double latitude, Double longitude, boolean openNow, List<OperatingHoursResponse> operatingHours) {

        static ClinicDetailResponse from(Clinic c, boolean openNow) {
            return new ClinicDetailResponse(c.id(), c.name(), c.address(), c.neighborhood(), c.phoneNumber(),
                    c.whatsappNumber(), c.logoUrl(), c.rating(), c.reviewCount(), c.verified(), c.latitude(),
                    c.longitude(), openNow, hours(c));
        }
    }

    record ExamTypeDetail(UUID id, String name, String category, String description) {
    }

    record ExamDetailResponse(UUID id, ExamTypeDetail examType, BigDecimal price, BigDecimal averagePrice,
            BigDecimal savings, boolean bestPrice, String preparation, String paymentMethods, Double distanceKm,
            ClinicDetailResponse clinic) {
    }

    record TrendingResponse(UUID id, String name, String slug, String category, BigDecimal minPrice, int offersCount,
            long recentContacts) {
    }

    record ClinicExamResponse(UUID id, ExamTypeSummary examType, BigDecimal price, boolean bestPrice,
            String preparation, String paymentMethods) {
    }

    record ClinicProfileResponse(UUID id, String name, String description, String address, String neighborhood,
            String city, String state, String phoneNumber, String whatsappNumber, String logoUrl, BigDecimal rating,
            int reviewCount, int patientCount, boolean isVerified, Double latitude, Double longitude, boolean openNow,
            List<OperatingHoursResponse> operatingHours, int examsCount, List<ClinicExamResponse> exams) {
    }
}
