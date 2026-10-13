package br.com.clinics.engagement;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import br.com.clinics.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class ContactAndTrendingIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper mapper;

    private UUID examId(String clinic, String type) {
        return jdbc.queryForObject("""
                SELECT e.id FROM exams e JOIN clinics c ON c.id = e.clinic_id JOIN exam_types t ON t.id = e.exam_type_id
                WHERE c.name = ? AND t.name = ?""", UUID.class, clinic, type);
    }

    private String registerAndGetToken() throws Exception {
        String phone = "(81) 9%04d-%04d".formatted(ThreadLocalRandom.current().nextInt(10000), ThreadLocalRandom.current().nextInt(10000));
        String json = mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(br.com.clinics.TestUsers.registrationJson("Maria", phone, "Senha1234")))
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(json).get("token").asString();
    }

    @Test
    void visitorContactReturnsWhatsappLinkAndStoresEventWithoutUser() throws Exception {
        UUID id = examId("Clínica Santa Maria", "Ultrassom Abdominal Total");

        String json = mvc.perform(post("/api/v1/exams/{id}/contacts", id))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.whatsapp_url").value(org.hamcrest.Matchers.startsWith("https://wa.me/5581999999999?text=")))
                .andExpect(jsonPath("$.whatsapp_url").value(org.hamcrest.Matchers.containsString("Ultrassom%20Abdominal%20Total")))
                .andExpect(jsonPath("$.estimated_savings").value(28.00))
                .andReturn().getResponse().getContentAsString();

        UUID contactId = UUID.fromString(mapper.readTree(json).get("contact_id").asString());
        var row = jdbc.queryForMap("SELECT user_id, price_at_contact, estimated_savings FROM contact_events WHERE id = ?", contactId);
        assertThat(row.get("user_id")).isNull();
        assertThat(row.get("price_at_contact")).hasToString("90.00");
        assertThat(row.get("estimated_savings")).hasToString("28.00");
    }

    @Test
    void authenticatedContactLinksTheEventToTheUser() throws Exception {
        UUID id = examId("Laboratório São José", "Hemograma Completo");
        String token = registerAndGetToken();

        String json = mvc.perform(post("/api/v1/exams/{id}/contacts", id).header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID contactId = UUID.fromString(mapper.readTree(json).get("contact_id").asString());
        assertThat(jdbc.queryForObject("SELECT user_id FROM contact_events WHERE id = ?", UUID.class, contactId)).isNotNull();
    }

    @Test
    void contactOnTheMostExpensiveOfferSavesNothing() throws Exception {
        UUID id = examId("Policlínica do Agreste", "Ultrassom Abdominal Total");

        mvc.perform(post("/api/v1/exams/{id}/contacts", id))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estimated_savings").value(0.00));
    }

    @Test
    void unknownExamIs404() throws Exception {
        mvc.perform(post("/api/v1/exams/{id}/contacts", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EXAM_NOT_FOUND"));
    }

    @Test
    void invalidTokenIsRejectedEvenOnPublicRoutes() throws Exception {
        mvc.perform(post("/api/v1/exams/{id}/contacts", UUID.randomUUID()).header("Authorization", "Bearer lixo"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void trendingReturnsAtMostFiveTypesWithStartingPrice() throws Exception {
        mvc.perform(get("/api/v1/exam-types/trending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].min_price").isNumber())
                .andExpect(jsonPath("$[0].offers_count").isNumber())
                .andExpect(jsonPath("$[0].category").isString());
    }

    @Test
    void trendingRanksByRecentContacts() throws Exception {
        UUID target = examId("Cardiocenter Surubim", "MAPA 24 horas");
        jdbc.update("""
                INSERT INTO contact_events (exam_id, price_at_contact, estimated_savings, created_at)
                SELECT ?, 130, 0, now() - interval '1 day' FROM generate_series(1, 500)""", target);
        try {
            mvc.perform(get("/api/v1/exam-types/trending"))
                    .andExpect(jsonPath("$[0].name").value("MAPA 24 horas"))
                    .andExpect(jsonPath("$[0].min_price").value(130.00));
        } finally {
            jdbc.update("DELETE FROM contact_events WHERE exam_id = ? AND price_at_contact = 130 AND created_at > now() - interval '2 days'", target);
        }
    }

    @Test
    void contactsOlderThanSevenDaysDoNotCount() throws Exception {
        UUID target = examId("Cardiocenter Surubim", "Teste Ergométrico");
        jdbc.update("""
                INSERT INTO contact_events (exam_id, price_at_contact, estimated_savings, created_at)
                SELECT ?, 150, 0, now() - interval '10 days' FROM generate_series(1, 500)""", target);
        try {
            // 500 contatos antigos não podem colocar o exame no topo (o seed tem no máximo 8 contatos por oferta)
            mvc.perform(get("/api/v1/exam-types/trending"))
                    .andExpect(jsonPath("$[0].name").value(org.hamcrest.Matchers.not("Teste Ergométrico")))
                    .andExpect(jsonPath("$[0].recent_contacts").value(org.hamcrest.Matchers.lessThan(100)));
        } finally {
            jdbc.update("DELETE FROM contact_events WHERE exam_id = ? AND price_at_contact = 150 AND created_at < now() - interval '9 days'", target);
        }
    }
}
