package br.com.clinics.rules;

import java.util.List;

import br.com.clinics.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/** Garante que os dados de demonstração respeitam as regras de negócio e o modelo de dados. */
@IntegrationTest
class SeedDataConsistencyIntegrationTest {

    @Autowired JdbcTemplate jdbc;

    private int count(String sql) {
        return jdbc.queryForObject(sql, Integer.class);
    }

    @Test
    @DisplayName("Premissa · o catálogo tem 5 categorias, 18 tipos de exame e várias clínicas de Surubim")
    void catalogHasExpectedSize() {
        assertThat(count("SELECT count(DISTINCT category) FROM exam_types")).isEqualTo(5);
        assertThat(count("SELECT count(*) FROM exam_types")).isEqualTo(18);
        assertThat(count("SELECT count(*) FROM clinics")).isGreaterThanOrEqualTo(6);
        assertThat(count("SELECT count(*) FROM clinics WHERE city <> 'Surubim' OR state <> 'PE'")).isZero();
    }

    @Test
    @DisplayName("RN28 · todo WhatsApp de clínica tem só dígitos com DDI 55")
    void whatsappNumbersAreDigitsOnly() {
        assertThat(count("SELECT count(*) FROM clinics WHERE whatsapp_number !~ '^55[0-9]{10,11}$'")).isZero();
    }

    @Test
    @DisplayName("RN19 / RN22 · toda clínica tem coordenadas dentro de Surubim e horário de atendimento")
    void clinicsHaveLocationAndHours() {
        assertThat(count("""
                SELECT count(*) FROM clinics
                WHERE latitude IS NULL OR longitude IS NULL
                   OR latitude NOT BETWEEN -7.95 AND -7.75 OR longitude NOT BETWEEN -35.85 AND -35.65""")).isZero();
        assertThat(count("""
                SELECT count(*) FROM clinics c
                WHERE NOT EXISTS (SELECT 1 FROM clinic_operating_hours h WHERE h.clinic_id = c.id)""")).isZero();
    }

    @Test
    @DisplayName("RN38 · as clínicas estão distribuídas em vários bairros")
    void clinicsSpanSeveralNeighborhoods() {
        assertThat(count("SELECT count(DISTINCT neighborhood) FROM clinics WHERE neighborhood IS NOT NULL"))
                .isGreaterThanOrEqualTo(5);
        assertThat(count("SELECT count(*) FROM clinics WHERE neighborhood IS NULL")).isZero();
    }

    @Test
    @DisplayName("RN27 · nota e quantidade de avaliações da clínica batem com as avaliações gravadas")
    void clinicRatingMatchesReviews() {
        assertThat(count("""
                SELECT count(*) FROM clinics c
                JOIN (SELECT clinic_id, round(avg(rating), 1) AS avg_rating, count(*) AS total
                      FROM reviews GROUP BY clinic_id) r ON r.clinic_id = c.id
                WHERE c.rating <> r.avg_rating OR c.review_count <> r.total""")).isZero();
    }

    @Test
    @DisplayName("RN13 · toda oferta tem ao menos uma linha no histórico de preços")
    void everyOfferHasPriceHistory() {
        assertThat(count("""
                SELECT count(*) FROM exams e
                WHERE NOT EXISTS (SELECT 1 FROM price_history p WHERE p.exam_id = e.id)""")).isZero();
    }

    @Test
    @DisplayName("RN39 · todo tipo de exame tem apelidos populares")
    void everyExamTypeHasAliases() {
        List<String> without = jdbc.queryForList("""
                SELECT t.name FROM exam_types t
                WHERE NOT EXISTS (SELECT 1 FROM exam_type_aliases a WHERE a.exam_type_id = t.id)""", String.class);
        assertThat(without).isEmpty();
    }

    @Test
    @DisplayName("RN09 · todo tipo de exame é oferecido por pelo menos uma clínica")
    void everyExamTypeHasAnOffer() {
        assertThat(count("""
                SELECT count(*) FROM exam_types t
                WHERE NOT EXISTS (SELECT 1 FROM exams e WHERE e.exam_type_id = t.id AND e.available)""")).isZero();
    }

    @Test
    @DisplayName("RN11 · os preços do seed são positivos e razoáveis")
    void seedPricesAreSane() {
        assertThat(count("SELECT count(*) FROM exams WHERE price <= 0 OR price > 2000")).isZero();
    }

    @Test
    @DisplayName("RN14 / RN15 · todo exame com mais de uma oferta tem preços diferentes para comparar")
    void comparableExamsHaveDistinctPrices() {
        assertThat(count("""
                SELECT count(*) FROM (
                  SELECT exam_type_id FROM exams WHERE available
                  GROUP BY exam_type_id HAVING count(*) > 1 AND min(price) = max(price)) x""")).isZero();
    }

    @Test
    @DisplayName("RN30 · há contatos recentes suficientes para montar o ranking Em alta")
    void enoughRecentContactsForTrending() {
        assertThat(count("""
                SELECT count(DISTINCT e.exam_type_id) FROM contact_events c JOIN exams e ON e.id = c.exam_id
                WHERE c.created_at >= now() - interval '7 days'""")).isGreaterThanOrEqualTo(5);
    }

    @Test
    @DisplayName("RN05 · nenhum usuário do seed tem papel privilegiado nem pode fazer login")
    void seedUsersHaveNoPrivilegedRole() {
        assertThat(count("SELECT count(*) FROM users WHERE role <> 'PATIENT' AND phone LIKE '5500%'")).isZero();
    }
}
