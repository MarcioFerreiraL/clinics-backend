package br.com.clinics.catalog;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Clock;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import br.com.clinics.common.ApiException;
import br.com.clinics.common.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Leitura do catálogo: busca e comparação de ofertas, detalhes, perfil da clínica, contato e exames em alta. */
@Service
public class CatalogService {

    /** Uma oferta: um tipo de exame vendido por uma clínica. */
    record Offer(UUID examId, UUID examTypeId, String examName, Category category, String examDescription,
            UUID clinicId, String clinicName, String neighborhood, Double latitude, Double longitude,
            BigDecimal clinicRating, boolean clinicVerified, String whatsappNumber, String logoUrl,
            BigDecimal price, String preparation, String paymentMethods, BigDecimal averagePrice, BigDecimal minPrice) {

        static Offer from(ExamRepository.OfferRow r) {
            boolean hasLocation = r.getLatitude() != null && r.getLongitude() != null;
            return new Offer(r.getExamId(), r.getExamTypeId(), r.getExamName(), Category.valueOf(r.getCategory()),
                    r.getExamDescription(), r.getClinicId(), r.getClinicName(), r.getNeighborhood(),
                    hasLocation ? r.getLatitude().doubleValue() : null, hasLocation ? r.getLongitude().doubleValue() : null,
                    r.getClinicRating(), r.getClinicVerified(), r.getWhatsappNumber(), r.getLogoUrl(), r.getPrice(),
                    r.getPreparation(), r.getPaymentMethods(), r.getAveragePrice().setScale(2, RoundingMode.HALF_UP),
                    r.getMinPrice());
        }

        /** Economia em relação à média local, nunca negativa. */
        BigDecimal savings() {
            return averagePrice.subtract(price).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        }

        /** Selo "Melhor preço": a(s) oferta(s) de menor preço do tipo. */
        boolean bestPrice() {
            return price.compareTo(minPrice) == 0;
        }

        /** Distância em km com uma casa decimal (Haversine), ou {@code null} sem coordenadas. */
        Double distanceKm(double[] origin) {
            if (origin == null || latitude == null || longitude == null) {
                return null;
            }
            double dLat = Math.toRadians(latitude - origin[0]);
            double dLng = Math.toRadians(longitude - origin[1]);
            double a = Math.pow(Math.sin(dLat / 2), 2) + Math.cos(Math.toRadians(origin[0]))
                    * Math.cos(Math.toRadians(latitude)) * Math.pow(Math.sin(dLng / 2), 2);
            double km = 6371.0088 * 2 * Math.asin(Math.sqrt(a));
            return BigDecimal.valueOf(km).setScale(1, RoundingMode.HALF_UP).doubleValue();
        }
    }

    record SearchItem(Offer offer, Double distanceKm) {
    }

    record Trending(UUID id, String name, String slug, Category category, BigDecimal minPrice, int offersCount,
            long recentContacts) {
    }

    record Contact(UUID contactId, String whatsappUrl, BigDecimal estimatedSavings) {
    }

    record Neighborhood(String name, int clinicsCount) {
    }

    private final ExamRepository exams;
    private final ClinicRepository clinics;
    private final ContactEventRepository contacts;
    private final ExamViewEventRepository views;
    private final Clock clock;

    public CatalogService(ExamRepository exams, ClinicRepository clinics, ContactEventRepository contacts,
            ExamViewEventRepository views, Clock clock) {
        this.exams = exams;
        this.clinics = clinics;
        this.contacts = contacts;
        this.views = views;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Page<SearchItem> search(String text, String category, String sortBy, Double lat, Double lng,
            Integer page, Integer size, String neighborhood, BigDecimal minPrice, BigDecimal maxPrice,
            BigDecimal minRating, Boolean bestPriceOnly) {
        Category cat = isBlank(category) ? null : Category.parse(category);
        Comparator<SearchItem> sort = sort(sortBy);
        double[] origin = origin(lat, lng);
        if ("distance".equalsIgnoreCase(strip(sortBy)) && origin == null) {
            throw ApiException.badRequest("LOCATION_REQUIRED", "Informe lat e lng para ordenar por distância.");
        }
        if ((minPrice != null && minPrice.signum() < 0) || (maxPrice != null && maxPrice.signum() < 0)
                || (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0)) {
            throw ApiException.badRequest("INVALID_PRICE_RANGE", "A faixa de preço é inválida.");
        }
        if (minRating != null && (minRating.signum() < 0 || minRating.compareTo(BigDecimal.valueOf(5)) > 0)) {
            throw ApiException.badRequest("INVALID_MIN_RATING", "A nota mínima deve estar entre 0 e 5.");
        }
        String wantedNeighborhood = isBlank(neighborhood) ? null : fold(neighborhood);
        String query = isBlank(text) ? null : text.strip();

        // Poucas ofertas por cidade: filtrar, ordenar e paginar em memória mantém as regras simples.
        List<SearchItem> items = exams.searchOffers(cat == null ? null : cat.name(),
                        query == null ? null : "%" + escapeLike(query) + "%", query)
                .stream()
                .map(Offer::from)
                .filter(o -> wantedNeighborhood == null || (o.neighborhood() != null && fold(o.neighborhood()).equals(wantedNeighborhood)))
                .filter(o -> minPrice == null || o.price().compareTo(minPrice) >= 0)
                .filter(o -> maxPrice == null || o.price().compareTo(maxPrice) <= 0)
                .filter(o -> minRating == null || o.clinicRating().compareTo(minRating) >= 0)
                .filter(o -> !Boolean.TRUE.equals(bestPriceOnly) || o.bestPrice())
                .map(o -> new SearchItem(o, o.distanceKm(origin)))
                .sorted(sort)
                .toList();
        int p = Page.page(page);
        int s = Page.size(size);
        int from = (int) Math.min((long) p * s, items.size());
        return Page.of(items.subList(from, Math.min(from + s, items.size())), p, s, items.size());
    }

    @Transactional(readOnly = true)
    public Offer offer(UUID examId) {
        return exams.findOffer(examId).map(Offer::from).orElseThrow(() -> ApiException.notFound("EXAM_NOT_FOUND", "Exame não encontrado."));
    }

    /** Valida a origem informada no detalhe do exame (lat e lng juntos). */
    double[] originFor(Double lat, Double lng) {
        return origin(lat, lng);
    }

    public void recordView(UUID examId) {
        views.save(new ExamViewEvent(examId, clock.instant()));
    }

    /** Registra o contato com preço e economia congelados e devolve o link do WhatsApp. */
    @Transactional
    public Contact contact(UUID examId, UUID userId) {
        Offer offer = offer(examId);
        String digits = offer.whatsappNumber() == null ? "" : offer.whatsappNumber().replaceAll("\\D", "");
        if (digits.isEmpty()) {
            throw ApiException.badRequest("CLINIC_WITHOUT_WHATSAPP", "A clínica não informou um WhatsApp.");
        }
        String price = offer.price().setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
        String message = "Olá! Vi o exame %s por R$ %s no Clinics e gostaria de agendar.".formatted(offer.examName(), price);
        String url = "https://wa.me/" + digits + "?text="
                + URLEncoder.encode(message, StandardCharsets.UTF_8).replace("+", "%20");

        BigDecimal savings = offer.savings();
        ContactEvent event = contacts.saveAndFlush(new ContactEvent(examId, userId, offer.price(), savings, clock.instant()));
        return new Contact(event.id(), url, savings);
    }

    /** Até 5 tipos de exame mais contatados nos últimos 7 dias. */
    @Transactional(readOnly = true)
    public List<Trending> trending() {
        return exams.findTrending(clock.instant().minus(Duration.ofDays(7))).stream()
                .map(t -> new Trending(t.getId(), t.getName(), t.getSlug(), Category.valueOf(t.getCategory()),
                        t.getMinPrice(), (int) t.getOffersCount(), t.getRecentContacts()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Clinic clinic(UUID id) {
        return clinics.findById(id)
                .orElseThrow(() -> ApiException.notFound("CLINIC_NOT_FOUND", "Clínica não encontrada."));
    }

    public boolean isOpenNow(Clinic clinic) {
        return clinic.isOpenAt(ZonedDateTime.now(clock));
    }

    @Transactional(readOnly = true)
    public List<Offer> offersOfClinic(UUID clinicId) {
        return exams.findOffersOfClinic(clinicId).stream().map(Offer::from).toList();
    }

    @Transactional(readOnly = true)
    public List<Neighborhood> neighborhoods() {
        return clinics.findNeighborhoods().stream()
                .map(n -> new Neighborhood(n.getName(), (int) n.getClinicsCount()))
                .toList();
    }

    /** Ordenações: price (padrão), rating, name ou distance. Empates desempatam por nome e depois por preço. */
    private static Comparator<SearchItem> sort(String sortBy) {
        Comparator<SearchItem> byName = Comparator.comparing(i -> i.offer().examName(), String.CASE_INSENSITIVE_ORDER);
        Comparator<SearchItem> byPrice = Comparator.comparing(i -> i.offer().price());
        return switch (isBlank(sortBy) ? "price" : sortBy.strip().toLowerCase()) {
            case "price" -> byPrice.thenComparing(byName);
            case "rating" -> Comparator.<SearchItem, BigDecimal>comparing(i -> i.offer().clinicRating()).reversed()
                    .thenComparing(byName).thenComparing(byPrice);
            case "name" -> byName.thenComparing(byPrice);
            case "distance" -> Comparator.comparing(SearchItem::distanceKm, Comparator.nullsLast(Comparator.<Double>naturalOrder()))
                    .thenComparing(byName).thenComparing(byPrice);
            default -> throw ApiException.badRequest("INVALID_SORT", "Ordenação inválida. Use: price, rating, name ou distance.");
        };
    }

    private static double[] origin(Double lat, Double lng) {
        if (lat == null && lng == null) {
            return null;
        }
        if (lat == null || lng == null) {
            throw ApiException.badRequest("LOCATION_INCOMPLETE", "Informe lat e lng juntos.");
        }
        if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            throw ApiException.badRequest("INVALID_COORDINATES", "Latitude ou longitude fora do intervalo válido.");
        }
        return new double[] {lat, lng};
    }

    /** Ignora caixa e acentos. */
    static String fold(String text) {
        return Normalizer.normalize(text.strip(), Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase();
    }

    private static String escapeLike(String text) {
        return text.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String strip(String value) {
        return value == null ? null : value.strip();
    }
}
