package br.com.clinics.catalog;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.Normalizer;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.com.clinics.common.ApiException;
import br.com.clinics.common.Page;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Leitura do catálogo: busca e comparação de ofertas, detalhes, perfil da clínica, contato e exames em alta. */
@Service
public class CatalogService {

    /**
     * Ofertas disponíveis com a média e o menor preço do mesmo tipo de exame.
     * A média é calculada sobre todas as ofertas do tipo, antes de qualquer filtro da busca.
     */
    private static final String OFFERS = """
            WITH offers AS (
                SELECT e.id AS exam_id, t.id AS exam_type_id, t.name AS exam_name, t.category,
                       COALESCE(e.description, t.description) AS exam_description,
                       c.id AS clinic_id, c.name AS clinic_name, c.neighborhood, c.latitude, c.longitude,
                       c.rating, c.is_verified, c.whatsapp_number, c.logo_url,
                       e.price, e.preparation, e.payment_methods,
                       AVG(e.price) OVER (PARTITION BY e.exam_type_id) AS average_price,
                       MIN(e.price) OVER (PARTITION BY e.exam_type_id) AS min_price
                FROM exams e
                JOIN exam_types t ON t.id = e.exam_type_id
                JOIN clinics c ON c.id = e.clinic_id
                WHERE e.available
            )
            SELECT * FROM offers
            """;

    /** Busca sem acento pelo nome do exame ou pelos nomes populares (apelidos). */
    private static final String SEARCH = OFFERS + """
            WHERE (:category IS NULL OR category = :category)
              AND (:pattern IS NULL
                   OR unaccent(lower(exam_name)) LIKE unaccent(lower(:pattern)) ESCAPE '\\'
                   OR EXISTS (SELECT 1 FROM exam_type_aliases a
                              WHERE a.exam_type_id = offers.exam_type_id
                                AND (unaccent(lower(a.alias)) LIKE unaccent(lower(:pattern)) ESCAPE '\\'
                                     OR unaccent(lower(:text)) LIKE '%' || unaccent(lower(a.alias)) || '%')))
            """;

    private static final String TRENDING = """
            SELECT t.id, t.name, t.slug, t.category,
                   MIN(e.price) AS min_price, COUNT(e.id) AS offers_count, COALESCE(c.contacts, 0) AS recent_contacts
            FROM exam_types t
            JOIN exams e ON e.exam_type_id = t.id AND e.available
            LEFT JOIN (
                SELECT ex.exam_type_id, COUNT(*) AS contacts
                FROM contact_events ce JOIN exams ex ON ex.id = ce.exam_id
                WHERE ce.created_at >= :since
                GROUP BY ex.exam_type_id
            ) c ON c.exam_type_id = t.id
            GROUP BY t.id, t.name, t.slug, t.category, c.contacts
            ORDER BY recent_contacts DESC, AVG(e.price) ASC, t.name
            LIMIT 5
            """;

    /** Uma oferta: um tipo de exame vendido por uma clínica. */
    record Offer(UUID examId, UUID examTypeId, String examName, Category category, String examDescription,
            UUID clinicId, String clinicName, String neighborhood, Double latitude, Double longitude,
            BigDecimal clinicRating, boolean clinicVerified, String whatsappNumber, String logoUrl,
            BigDecimal price, String preparation, String paymentMethods, BigDecimal averagePrice, BigDecimal minPrice) {

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

    record Hours(int dayOfWeek, LocalTime opensAt, LocalTime closesAt) {
    }

    record Clinic(UUID id, String name, String description, String address, String neighborhood, String city,
            String state, Double latitude, Double longitude, String phoneNumber, String whatsappNumber, String logoUrl,
            BigDecimal rating, int reviewCount, int patientCount, boolean verified, List<Hours> hours) {

        /** Aberta se o momento cai em algum intervalo do dia (abre inclusive, fecha exclusive). */
        boolean openAt(ZonedDateTime moment) {
            LocalTime time = moment.toLocalTime();
            return hours.stream().anyMatch(h -> moment.getDayOfWeek() == DayOfWeek.of(h.dayOfWeek())
                    && !time.isBefore(h.opensAt()) && time.isBefore(h.closesAt()));
        }
    }

    record Trending(UUID id, String name, String slug, Category category, BigDecimal minPrice, int offersCount,
            long recentContacts) {
    }

    record Contact(UUID contactId, String whatsappUrl, BigDecimal estimatedSavings) {
    }

    record Neighborhood(String name, int clinicsCount) {
    }

    private final JdbcClient jdbc;
    private final Clock clock;

    public CatalogService(JdbcClient jdbc, Clock clock) {
        this.jdbc = jdbc;
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
        List<SearchItem> items = jdbc.sql(SEARCH)
                .param("category", cat == null ? null : cat.name(), Types.VARCHAR)
                .param("pattern", query == null ? null : "%" + escapeLike(query) + "%", Types.VARCHAR)
                .param("text", query, Types.VARCHAR)
                .query(CatalogService::mapOffer)
                .stream()
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
        return findOffer(examId).orElseThrow(() -> ApiException.notFound("EXAM_NOT_FOUND", "Exame não encontrado."));
    }

    /** Valida a origem informada no detalhe do exame (lat e lng juntos). */
    double[] originFor(Double lat, Double lng) {
        return origin(lat, lng);
    }

    public void recordView(UUID examId) {
        jdbc.sql("INSERT INTO exam_view_events (exam_id, created_at) VALUES (?, ?)")
                .params(examId, Timestamp.from(clock.instant()))
                .update();
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

        UUID id = UUID.randomUUID();
        BigDecimal savings = offer.savings();
        jdbc.sql("""
                        INSERT INTO contact_events (id, exam_id, user_id, price_at_contact, estimated_savings, created_at)
                        VALUES (:id, :examId, :userId, :price, :savings, :createdAt)""")
                .param("id", id)
                .param("examId", examId)
                .param("userId", userId, Types.OTHER)
                .param("price", offer.price())
                .param("savings", savings)
                .param("createdAt", Timestamp.from(clock.instant()))
                .update();
        return new Contact(id, url, savings);
    }

    /** Até 5 tipos de exame mais contatados nos últimos 7 dias. */
    @Transactional(readOnly = true)
    public List<Trending> trending() {
        return jdbc.sql(TRENDING)
                .param("since", Timestamp.from(clock.instant().minus(Duration.ofDays(7))))
                .query((rs, row) -> new Trending(rs.getObject("id", UUID.class), rs.getString("name"),
                        rs.getString("slug"), Category.valueOf(rs.getString("category")), rs.getBigDecimal("min_price"),
                        rs.getInt("offers_count"), rs.getLong("recent_contacts")))
                .list();
    }

    @Transactional(readOnly = true)
    public Clinic clinic(UUID id) {
        List<Hours> hours = jdbc.sql("""
                        SELECT day_of_week, opens_at, closes_at FROM clinic_operating_hours
                        WHERE clinic_id = ? ORDER BY day_of_week, opens_at""")
                .param(id)
                .query((rs, row) -> new Hours(rs.getInt("day_of_week"), rs.getTime("opens_at").toLocalTime(),
                        rs.getTime("closes_at").toLocalTime()))
                .list();
        return jdbc.sql("SELECT * FROM clinics WHERE id = ?")
                .param(id)
                .query((rs, row) -> new Clinic(rs.getObject("id", UUID.class), rs.getString("name"),
                        rs.getString("description"), rs.getString("address"), rs.getString("neighborhood"),
                        rs.getString("city"), rs.getString("state"), toDouble(rs.getBigDecimal("latitude")),
                        toDouble(rs.getBigDecimal("longitude")), rs.getString("phone_number"),
                        rs.getString("whatsapp_number"), rs.getString("logo_url"), rs.getBigDecimal("rating"),
                        rs.getInt("review_count"), rs.getInt("patient_count"), rs.getBoolean("is_verified"), hours))
                .optional()
                .orElseThrow(() -> ApiException.notFound("CLINIC_NOT_FOUND", "Clínica não encontrada."));
    }

    public boolean isOpenNow(Clinic clinic) {
        return clinic.openAt(ZonedDateTime.now(clock));
    }

    @Transactional(readOnly = true)
    public List<Offer> offersOfClinic(UUID clinicId) {
        return jdbc.sql(OFFERS + " WHERE clinic_id = ? ORDER BY exam_name").param(clinicId)
                .query(CatalogService::mapOffer).list();
    }

    @Transactional(readOnly = true)
    public List<Neighborhood> neighborhoods() {
        return jdbc.sql("""
                        SELECT neighborhood, count(*) AS clinics_count FROM clinics
                        WHERE neighborhood IS NOT NULL
                        GROUP BY neighborhood ORDER BY unaccent(lower(neighborhood))""")
                .query((rs, row) -> new Neighborhood(rs.getString("neighborhood"), rs.getInt("clinics_count")))
                .list();
    }

    private Optional<Offer> findOffer(UUID examId) {
        return jdbc.sql(OFFERS + " WHERE exam_id = ?").param(examId).query(CatalogService::mapOffer).optional();
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
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String strip(String value) {
        return value == null ? null : value.strip();
    }

    private static Double toDouble(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

    private static Offer mapOffer(ResultSet rs, int row) throws SQLException {
        BigDecimal lat = rs.getBigDecimal("latitude");
        BigDecimal lng = rs.getBigDecimal("longitude");
        boolean hasLocation = lat != null && lng != null;
        return new Offer(rs.getObject("exam_id", UUID.class), rs.getObject("exam_type_id", UUID.class),
                rs.getString("exam_name"), Category.valueOf(rs.getString("category")), rs.getString("exam_description"),
                rs.getObject("clinic_id", UUID.class), rs.getString("clinic_name"), rs.getString("neighborhood"),
                hasLocation ? lat.doubleValue() : null, hasLocation ? lng.doubleValue() : null,
                rs.getBigDecimal("rating"), rs.getBoolean("is_verified"), rs.getString("whatsapp_number"),
                rs.getString("logo_url"), rs.getBigDecimal("price"), rs.getString("preparation"),
                rs.getString("payment_methods"), rs.getBigDecimal("average_price").setScale(2, RoundingMode.HALF_UP),
                rs.getBigDecimal("min_price"));
    }
}
