package br.com.clinics.engagement;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import br.com.clinics.IntegrationTest;
import br.com.clinics.TestUsers;
import br.com.clinics.TestUsers.TestUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Cada teste cria a própria clínica para não alterar as notas do seed usadas por outros testes. */
@IntegrationTest
class ReviewsIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper mapper;

    private final List<UUID> createdClinics = new ArrayList<>();

    private UUID newClinic() {
        UUID id = jdbc.queryForObject(
                "INSERT INTO clinics (name, address, whatsapp_number) VALUES ('Clínica de Teste', 'Rua X, 1', '5581999990000') RETURNING id",
                UUID.class);
        createdClinics.add(id);
        return id;
    }

    @AfterEach
    void cleanUp() {
        createdClinics.forEach(id -> jdbc.update("DELETE FROM clinics WHERE id = ?", id));
    }

    private ResultActions review(TestUser user, UUID clinic, String body) throws Exception {
        return mvc.perform(put("/api/v1/clinics/{id}/reviews/me", clinic).header("Authorization", user.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String body(int rating, String comment) {
        return "{\"rating\":%d,\"comment\":\"%s\"}".formatted(rating, comment);
    }

    @Test
    void firstReviewIsCreatedAndUpdatesTheClinicRating() throws Exception {
        UUID clinic = newClinic();
        TestUser user = TestUsers.register(mvc, mapper);

        review(user, clinic, body(5, "Excelente")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.comment").value("Excelente"));

        mvc.perform(get("/api/v1/clinics/{id}", clinic))
                .andExpect(jsonPath("$.rating").value(5.0))
                .andExpect(jsonPath("$.review_count").value(1));
    }

    @Test
    void averageIsRecalculatedAcrossPatients() throws Exception {
        UUID clinic = newClinic();
        review(TestUsers.register(mvc, mapper), clinic, body(5, "")).andExpect(status().isCreated());
        review(TestUsers.register(mvc, mapper), clinic, body(4, "")).andExpect(status().isCreated());
        review(TestUsers.register(mvc, mapper), clinic, body(4, "")).andExpect(status().isCreated());

        mvc.perform(get("/api/v1/clinics/{id}", clinic))
                .andExpect(jsonPath("$.rating").value(4.3))
                .andExpect(jsonPath("$.review_count").value(3));
    }

    @Test
    void reviewingAgainReplacesThePreviousOneInsteadOfAddingAnother() throws Exception {
        UUID clinic = newClinic();
        TestUser user = TestUsers.register(mvc, mapper);
        review(user, clinic, body(5, "Ótimo")).andExpect(status().isCreated());

        review(user, clinic, body(2, "Piorou")).andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(2))
                .andExpect(jsonPath("$.comment").value("Piorou"));

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM reviews WHERE clinic_id = ?", Integer.class, clinic)).isEqualTo(1);
        mvc.perform(get("/api/v1/clinics/{id}", clinic))
                .andExpect(jsonPath("$.rating").value(2.0))
                .andExpect(jsonPath("$.review_count").value(1));
    }

    @Test
    void rejectsInvalidRatingAndTooLongComment() throws Exception {
        UUID clinic = newClinic();
        TestUser user = TestUsers.register(mvc, mapper);

        review(user, clinic, body(0, "x")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_RATING"));
        review(user, clinic, body(6, "x")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_RATING"));
        review(user, clinic, "{}").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        review(user, clinic, body(5, "a".repeat(1001))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mvc.perform(get("/api/v1/clinics/{id}", clinic)).andExpect(jsonPath("$.review_count").value(0));
    }

    @Test
    void submittingRequiresAuthentication() throws Exception {
        mvc.perform(put("/api/v1/clinics/{id}/reviews/me", newClinic()).contentType(MediaType.APPLICATION_JSON).content(body(5, "x")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unknownClinicIs404() throws Exception {
        TestUser user = TestUsers.register(mvc, mapper);
        review(user, UUID.randomUUID(), body(5, "x")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CLINIC_NOT_FOUND"));
        mvc.perform(get("/api/v1/clinics/{id}/reviews", UUID.randomUUID())).andExpect(status().isNotFound());
    }

    @Test
    void publicListShowsAbbreviatedAuthorsNewestFirst() throws Exception {
        UUID clinic = newClinic();
        review(TestUsers.register(mvc, mapper), clinic, body(3, "Primeira")).andExpect(status().isCreated());
        Thread.sleep(5);
        review(TestUsers.register(mvc, mapper), clinic, body(5, "Segunda")).andExpect(status().isCreated());

        mvc.perform(get("/api/v1/clinics/{id}/reviews", clinic))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.data[0].comment").value("Segunda"))
                .andExpect(jsonPath("$.data[1].comment").value("Primeira"))
                .andExpect(jsonPath("$.data[0].author_name").value("Maria S."))
                .andExpect(jsonPath("$.data[0].user_id").doesNotExist())
                .andExpect(jsonPath("$.data[0].author_name").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Silva"))));
    }

    @Test
    void listPaginates() throws Exception {
        UUID clinic = newClinic();
        for (int i = 0; i < 3; i++) {
            review(TestUsers.register(mvc, mapper), clinic, body(4, "r" + i));
        }

        mvc.perform(get("/api/v1/clinics/{id}/reviews", clinic).param("size", "2"))
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.total_pages").value(2))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void myReviewIs404UntilIReviewAndItIsPrivate() throws Exception {
        UUID clinic = newClinic();
        TestUser user = TestUsers.register(mvc, mapper);
        TestUser other = TestUsers.register(mvc, mapper);

        mvc.perform(get("/api/v1/clinics/{id}/reviews/me", clinic).header("Authorization", user.bearer()))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("REVIEW_NOT_FOUND"));

        review(user, clinic, body(4, "Bom")).andExpect(status().isCreated());

        mvc.perform(get("/api/v1/clinics/{id}/reviews/me", clinic).header("Authorization", user.bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.rating").value(4));
        mvc.perform(get("/api/v1/clinics/{id}/reviews/me", clinic).header("Authorization", other.bearer()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/clinics/{id}/reviews/me", clinic)).andExpect(status().isUnauthorized());
    }
}
