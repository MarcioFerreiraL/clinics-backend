package br.com.clinics.clinic;

import java.util.UUID;

import br.com.clinics.IntegrationTest;
import br.com.clinics.TestUsers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class ClinicManagementIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper mapper;

    private UUID managerId;
    private UUID clinicId;
    private UUID createdExamId;

    @AfterEach
    void cleanUp() {
        if (createdExamId != null) {
            jdbc.update("DELETE FROM exams WHERE id = ?", createdExamId);
        }
        if (clinicId != null) {
            jdbc.update("UPDATE clinics SET owner_user_id = NULL WHERE id = ?", clinicId);
        }
        if (managerId != null) {
            jdbc.update("DELETE FROM users WHERE id = ?", managerId);
        }
    }

    @Test
    void clinicManagerCanManageOffersAndSeeRecentEngagement() throws Exception {
        TestUsers.TestUser user = TestUsers.register(mvc, mapper);
        managerId = user.id();
        clinicId = jdbc.queryForObject("SELECT id FROM clinics WHERE owner_user_id IS NULL LIMIT 1", UUID.class);
        jdbc.update("UPDATE users SET role = 'CLINIC_MANAGER' WHERE id = ?", managerId);
        jdbc.update("UPDATE clinics SET owner_user_id = ? WHERE id = ?", managerId, clinicId);
        String token = login(user.phone());

        mvc.perform(get("/api/v1/clinic/me/dashboard").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period_days").value(30))
                .andExpect(jsonPath("$.clinic_name").isString());
        mvc.perform(get("/api/v1/clinic/me/exam-catalog").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").isString());

        UUID examTypeId = jdbc.queryForObject("""
                SELECT t.id FROM exam_types t
                WHERE NOT EXISTS (SELECT 1 FROM exams e WHERE e.clinic_id = ? AND e.exam_type_id = t.id)
                ORDER BY t.name LIMIT 1""", UUID.class, clinicId);
        String request = """
                {"exam_type_id":"%s","price":55.50,"description":"Descrição da clínica",
                 "preparation":"Jejum de 8 horas","payment_methods":"Pix e cartão","available":true}
                """.formatted(examTypeId);
        String created = mvc.perform(post("/api/v1/clinic/me/exams")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exam_type.name").isString())
                .andExpect(jsonPath("$.description").value("Descrição da clínica"))
                .andReturn().getResponse().getContentAsString();
        createdExamId = UUID.fromString(mapper.readTree(created).get("id").asString());
        mvc.perform(post("/api/v1/clinic/me/exams")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EXAM_ALREADY_OFFERED"));
        jdbc.update("""
                INSERT INTO exam_view_events (exam_id, created_at)
                VALUES (?, now() - interval '31 days')""", createdExamId);
        jdbc.update("""
                INSERT INTO contact_events (exam_id, price_at_contact, estimated_savings, created_at)
                VALUES (?, 55.50, 0, now() - interval '31 days')""", createdExamId);

        String update = """
                {"exam_type_id":"%s","price":60.00,"description":"Descrição atualizada",
                 "preparation":"Sem preparo","payment_methods":"Cartão","available":true}
                """.formatted(examTypeId);
        mvc.perform(put("/api/v1/clinic/me/exams/{id}", createdExamId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(update))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(60.00))
                .andExpect(jsonPath("$.description").value("Descrição atualizada"));

        mvc.perform(get("/api/v1/exams/{id}", createdExamId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exam_type.description").value("Descrição atualizada"));
        mvc.perform(post("/api/v1/exams/{id}/contacts", createdExamId))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/v1/clinic/me/dashboard").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exam_views").value(1))
                .andExpect(jsonPath("$.whatsapp_clicks").value(1));

        mvc.perform(delete("/api/v1/clinic/me/exams/{id}", createdExamId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("SELECT available FROM exams WHERE id = ?", Boolean.class, createdExamId))
                .isFalse();
        String listed = mvc.perform(get("/api/v1/clinic/me/exams").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        boolean foundInactive = false;
        for (var item : mapper.readTree(listed).get("exams")) {
            if (item.get("id").asString().equals(createdExamId.toString())) {
                foundInactive = !item.get("available").asBoolean();
                break;
            }
        }
        assertThat(foundInactive).isTrue();

        UUID otherClinicExam = jdbc.queryForObject("""
                SELECT id FROM exams WHERE clinic_id <> ? LIMIT 1""", UUID.class, clinicId);
        mvc.perform(put("/api/v1/clinic/me/exams/{id}", otherClinicExam)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(update))
                .andExpect(status().isNotFound());
    }

    @Test
    void patientsCannotUseClinicManagementEndpoints() throws Exception {
        TestUsers.TestUser patient = TestUsers.register(mvc, mapper);
        mvc.perform(get("/api/v1/clinic/me/dashboard").header("Authorization", patient.bearer()))
                .andExpect(status().isForbidden());
    }

    private String login(String phone) throws Exception {
        String json = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"%s","password":"%s"}
                                """.formatted(phone, TestUsers.PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(json).get("token").asString();
    }
}
