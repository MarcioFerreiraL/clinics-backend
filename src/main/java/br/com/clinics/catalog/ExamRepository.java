package br.com.clinics.catalog;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * As consultas de ofertas são nativas porque usam funções de janela e {@code unaccent}.
 * Os apelidos entre aspas dão nome aos getters das projeções ({@code "examId"} preenche {@code getExamId()}).
 */
public interface ExamRepository extends JpaRepository<Exam, UUID> {

    /** Ofertas disponíveis com média e menor preço do tipo, calculados antes de qualquer filtro da busca. */
    String OFFERS = """
            WITH offers AS (
                SELECT e.id AS "examId", t.id AS "examTypeId", t.name AS "examName", t.category,
                       COALESCE(e.description, t.description) AS "examDescription",
                       c.id AS "clinicId", c.name AS "clinicName", c.neighborhood, c.latitude, c.longitude,
                       c.rating AS "clinicRating", c.is_verified AS "clinicVerified",
                       c.whatsapp_number AS "whatsappNumber", c.logo_url AS "logoUrl",
                       e.price, e.preparation, e.payment_methods AS "paymentMethods",
                       AVG(e.price) OVER (PARTITION BY e.exam_type_id) AS "averagePrice",
                       MIN(e.price) OVER (PARTITION BY e.exam_type_id) AS "minPrice"
                FROM exams e
                JOIN exam_types t ON t.id = e.exam_type_id
                JOIN clinics c ON c.id = e.clinic_id
                WHERE e.available
            )
            SELECT * FROM offers
            """;

    /** Busca sem acento pelo nome do exame ou pelos nomes populares (apelidos). */
    @Query(value = OFFERS + """
            WHERE (CAST(:category AS varchar) IS NULL OR category = CAST(:category AS varchar))
              AND (CAST(:pattern AS varchar) IS NULL
                   OR unaccent(lower("examName")) LIKE unaccent(lower(CAST(:pattern AS varchar))) ESCAPE '!'
                   OR EXISTS (SELECT 1 FROM exam_type_aliases a
                              WHERE a.exam_type_id = offers."examTypeId"
                                AND (unaccent(lower(a.alias)) LIKE unaccent(lower(CAST(:pattern AS varchar))) ESCAPE '!'
                                     OR unaccent(lower(CAST(:text AS varchar))) LIKE '%' || unaccent(lower(a.alias)) || '%')))
            """, nativeQuery = true)
    List<OfferRow> searchOffers(String category, String pattern, String text);

    @Query(value = OFFERS + " WHERE \"examId\" = :id", nativeQuery = true)
    Optional<OfferRow> findOffer(UUID id);

    @Query(value = OFFERS + " WHERE \"clinicId\" = :clinicId ORDER BY \"examName\"", nativeQuery = true)
    List<OfferRow> findOffersOfClinic(UUID clinicId);

    /** Os 5 tipos de exame mais contatados desde {@code since}. */
    @Query(value = """
            SELECT t.id, t.name, t.slug, t.category,
                   MIN(e.price) AS "minPrice", COUNT(e.id) AS "offersCount", COALESCE(c.contacts, 0) AS "recentContacts"
            FROM exam_types t
            JOIN exams e ON e.exam_type_id = t.id AND e.available
            LEFT JOIN (
                SELECT ex.exam_type_id, COUNT(*) AS contacts
                FROM contact_events ce JOIN exams ex ON ex.id = ce.exam_id
                WHERE ce.created_at >= :since
                GROUP BY ex.exam_type_id
            ) c ON c.exam_type_id = t.id
            GROUP BY t.id, t.name, t.slug, t.category, c.contacts
            ORDER BY "recentContacts" DESC, AVG(e.price) ASC, t.name
            LIMIT 5""", nativeQuery = true)
    List<TrendingRow> findTrending(Instant since);

    /** Todas as ofertas da clínica, inclusive as desativadas. */
    @Query(value = """
            SELECT e.id, t.id AS "examTypeId", t.name, t.category,
                   COALESCE(e.description, t.description) AS description,
                   e.price, e.preparation, e.payment_methods AS "paymentMethods", e.available
            FROM exams e JOIN exam_types t ON t.id = e.exam_type_id
            WHERE e.clinic_id = :clinicId
            ORDER BY unaccent(lower(t.name))""", nativeQuery = true)
    List<ManagedExamRow> findManagedExams(UUID clinicId);

    Optional<Exam> findByIdAndClinicId(UUID id, UUID clinicId);

    long countByClinicIdAndAvailableTrue(UUID clinicId);

    interface OfferRow {

        UUID getExamId();

        UUID getExamTypeId();

        String getExamName();

        String getCategory();

        String getExamDescription();

        UUID getClinicId();

        String getClinicName();

        String getNeighborhood();

        BigDecimal getLatitude();

        BigDecimal getLongitude();

        BigDecimal getClinicRating();

        boolean getClinicVerified();

        String getWhatsappNumber();

        String getLogoUrl();

        BigDecimal getPrice();

        String getPreparation();

        String getPaymentMethods();

        BigDecimal getAveragePrice();

        BigDecimal getMinPrice();
    }

    interface TrendingRow {

        UUID getId();

        String getName();

        String getSlug();

        String getCategory();

        BigDecimal getMinPrice();

        long getOffersCount();

        long getRecentContacts();
    }

    interface ManagedExamRow {

        UUID getId();

        UUID getExamTypeId();

        String getName();

        String getCategory();

        String getDescription();

        BigDecimal getPrice();

        String getPreparation();

        String getPaymentMethods();

        boolean getAvailable();
    }
}
