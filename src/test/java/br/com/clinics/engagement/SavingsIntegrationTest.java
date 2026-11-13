package br.com.clinics.engagement;

import java.util.UUID;

import br.com.clinics.IntegrationTest;
import br.com.clinics.TestUsers;
import br.com.clinics.TestUsers.TestUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Seed: Ultrassom Abdominal na Santa Maria custa R$ 90 (média 118, economia 28); na Policlínica custa R$ 140 (economia 0). */
@IntegrationTest
class SavingsIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper mapper;

    private UUID examId(String clinic) {
        return jdbc.queryForObject("""
                SELECT e.id FROM exams e JOIN clinics c ON c.id = e.clinic_id JOIN exam_types t ON t.id = e.exam_type_id
                WHERE c.name = ? AND t.name = 'Ultrassom Abdominal Total'""", UUID.class, clinic);
    }

    private void contact(TestUser user, UUID exam) throws Exception {
        var request = post("/api/v1/exams/{id}/contacts", exam);
        if (user != null) {
            request.header("Authorization", user.bearer());
        }
        mvc.perform(request).andExpect(status().isCreated());
    }

    @Test
    void savingsRequireAuthentication() throws Exception {
        mvc.perform(get("/api/v1/me/savings")).andExpect(status().isUnauthorized());
    }

    @Test
    void newPatientHasSavedNothing() throws Exception {
        TestUser user = TestUsers.register(mvc, mapper);

        mvc.perform(get("/api/v1/me/savings").header("Authorization", user.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_savings").value(0.00))
                .andExpect(jsonPath("$.contacts_count").value(0));
    }

    @Test
    void sumsTheSavingsOfEveryContact() throws Exception {
        TestUser user = TestUsers.register(mvc, mapper);
        contact(user, examId("Clínica Santa Mariazinha"));
        contact(user, examId("Clínica Santa Mariazinha"));
        contact(user, examId("Policlínica do Agrestão"));

        mvc.perform(get("/api/v1/me/savings").header("Authorization", user.bearer()))
                .andExpect(jsonPath("$.total_savings").value(56.00))
                .andExpect(jsonPath("$.contacts_count").value(3));
    }

    @Test
    void ignoresVisitorContactsAndOtherUsers() throws Exception {
        TestUser user = TestUsers.register(mvc, mapper);
        TestUser other = TestUsers.register(mvc, mapper);
        contact(user, examId("Clínica Santa Mariazinha"));
        contact(null, examId("Clínica Santa Mariazinha"));
        contact(other, examId("Clínica Santa Mariazinha"));

        mvc.perform(get("/api/v1/me/savings").header("Authorization", user.bearer()))
                .andExpect(jsonPath("$.total_savings").value(28.00))
                .andExpect(jsonPath("$.contacts_count").value(1));
    }

    @Test
    void savingsAreFrozenWhenThePriceChangesLater() throws Exception {
        TestUser user = TestUsers.register(mvc, mapper);
        UUID exam = examId("Clínica Santa Mariazinha");
        contact(user, exam);

        jdbc.update("UPDATE exams SET price = 130 WHERE id = ?", exam);
        try {
            mvc.perform(get("/api/v1/me/savings").header("Authorization", user.bearer()))
                    .andExpect(jsonPath("$.total_savings").value(28.00));
        } finally {
            jdbc.update("UPDATE exams SET price = 90 WHERE id = ?", exam);
        }
    }
}
