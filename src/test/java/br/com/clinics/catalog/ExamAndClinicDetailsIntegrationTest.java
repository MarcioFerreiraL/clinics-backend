package br.com.clinics.catalog;

import java.util.UUID;

import br.com.clinics.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class ExamAndClinicDetailsIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    private UUID examId(String clinic, String type) {
        return jdbc.queryForObject("""
                SELECT e.id FROM exams e JOIN clinics c ON c.id = e.clinic_id JOIN exam_types t ON t.id = e.exam_type_id
                WHERE c.name = ? AND t.name = ?""", UUID.class, clinic, type);
    }

    private UUID clinicId(String name) {
        return jdbc.queryForObject("SELECT id FROM clinics WHERE name = ?", UUID.class, name);
    }

    @Test
    void showsExamDetailsWithClinicAndComparison() throws Exception {
        UUID id = examId("Clínica Santa Maria", "Ultrassom Abdominal Total");

        mvc.perform(get("/api/v1/exams/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exam_type.name").value("Ultrassom Abdominal Total"))
                .andExpect(jsonPath("$.exam_type.category").value("Ultrassom"))
                .andExpect(jsonPath("$.exam_type.description").isNotEmpty())
                .andExpect(jsonPath("$.price").value(90.00))
                .andExpect(jsonPath("$.average_price").value(118.00))
                .andExpect(jsonPath("$.best_price").value(true))
                .andExpect(jsonPath("$.preparation").value("Jejum 8h • Laudo em 24h"))
                .andExpect(jsonPath("$.clinic.name").value("Clínica Santa Maria"))
                .andExpect(jsonPath("$.clinic.whatsapp_number").value("5581999999999"))
                .andExpect(jsonPath("$.clinic.open_now").isBoolean())
                .andExpect(jsonPath("$.clinic.operating_hours.length()").value(6))
                .andExpect(jsonPath("$.distance_km").doesNotExist());
    }

    @Test
    void includesDistanceWhenLocationIsGiven() throws Exception {
        UUID id = examId("Clínica Santa Maria", "Ultrassom Abdominal Total");

        mvc.perform(get("/api/v1/exams/{id}", id).param("lat", "-7.8335").param("lng", "-35.7530"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.distance_km").isNumber());
    }

    @Test
    void unknownExamIs404() throws Exception {
        mvc.perform(get("/api/v1/exams/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EXAM_NOT_FOUND"));
    }

    @Test
    void unavailableExamIs404() throws Exception {
        UUID id = examId("Cardiocenter Surubim", "MAPA 24 horas");
        jdbc.update("UPDATE exams SET available = FALSE WHERE id = ?", id);
        try {
            mvc.perform(get("/api/v1/exams/{id}", id)).andExpect(status().isNotFound());
        } finally {
            jdbc.update("UPDATE exams SET available = TRUE WHERE id = ?", id);
        }
    }

    @Test
    void malformedIdIs400() throws Exception {
        mvc.perform(get("/api/v1/exams/nao-e-uuid")).andExpect(status().isBadRequest());
    }

    @Test
    void showsPublicClinicProfileWithExams() throws Exception {
        mvc.perform(get("/api/v1/clinics/{id}", clinicId("Clínica Santa Maria")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Clínica Santa Maria"))
                .andExpect(jsonPath("$.rating").value(4.9))
                .andExpect(jsonPath("$.review_count").value(128))
                .andExpect(jsonPath("$.is_verified").value(true))
                .andExpect(jsonPath("$.exams_count").value(5))
                .andExpect(jsonPath("$.exams.length()").value(5))
                .andExpect(jsonPath("$.exams[0].exam_type.name").value("Eletrocardiograma (ECG)"))
                .andExpect(jsonPath("$.operating_hours.length()").value(6))
                .andExpect(jsonPath("$.operating_hours[0].day_of_week").value(1))
                .andExpect(jsonPath("$.operating_hours[0].opens_at").value("06:30"));
    }

    @Test
    void unknownClinicIs404() throws Exception {
        mvc.perform(get("/api/v1/clinics/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CLINIC_NOT_FOUND"));
    }
}
