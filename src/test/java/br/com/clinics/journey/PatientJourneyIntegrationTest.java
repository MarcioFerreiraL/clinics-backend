package br.com.clinics.journey;

import java.util.Base64;
import java.util.UUID;

import br.com.clinics.IntegrationTest;
import br.com.clinics.TestUsers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Aceitação dos requisitos funcionais RF01 a RF12 (docs/01-requisitos.md) numa jornada única,
 * do cadastro ao perfil. Roda em transação desfeita no fim.
 */
@IntegrationTest
@Transactional
class PatientJourneyIntegrationTest {

    // PNG de 1x1 pixel
    private static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==");

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper mapper;

    private JsonNode json(String body) throws Exception {
        return mapper.readTree(body);
    }

    @Test
    @DisplayName("RF01–RF12 · jornada completa: cadastro, busca, detalhes, favorito, contato, avaliação e perfil")
    void fullPatientJourney() throws Exception {
        // RF01 · cadastro com nome, telefone, CPF, e-mail, nascimento e senha
        String phone = "(81) 9%04d-%04d".formatted((int) (Math.random() * 10000), (int) (Math.random() * 10000));
        String registration = TestUsers.registrationJson("Maria da Silva", phone, "Senha1234");
        JsonNode registered = json(mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registration)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        assertThat(registered.get("token").asString()).isNotBlank();

        // RF02 · login com telefone e senha devolve token
        JsonNode login = json(mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"%s\",\"password\":\"Senha1234\"}".formatted(phone)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        String bearer = "Bearer " + login.get("token").asString();

        // RF03 + RF04 · busca por nome popular, com média local e economia
        JsonNode search = json(mvc.perform(get("/api/v1/exams/search").param("query", "ultrassom de barriga")
                        .param("sort_by", "price")).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(5)).andExpect(jsonPath("$.data[0].average_price").value(118.00))
                .andExpect(jsonPath("$.data[0].savings").value(28.00)).andReturn().getResponse().getContentAsString());
        JsonNode cheapest = search.get("data").get(0);
        String examId = cheapest.get("id").asString();
        String examTypeId = cheapest.get("exam_type").get("id").asString();
        String clinicId = cheapest.get("clinic").get("id").asString();
        assertThat(cheapest.get("best_price").asBoolean()).isTrue();

        // RF05 · detalhes da oferta: preparo, pagamento, clínica e "aberta agora"
        mvc.perform(get("/api/v1/exams/{id}", examId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.preparation").isNotEmpty()).andExpect(jsonPath("$.payment_methods").isNotEmpty())
                .andExpect(jsonPath("$.clinic.open_now").isBoolean())
                .andExpect(jsonPath("$.clinic.operating_hours").isNotEmpty());

        // RF06 · perfil público da clínica
        mvc.perform(get("/api/v1/clinics/{id}", clinicId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").exists()).andExpect(jsonPath("$.review_count").exists())
                .andExpect(jsonPath("$.exams_count").isNumber()).andExpect(jsonPath("$.operating_hours").isNotEmpty());

        // RF07 · exames em alta
        mvc.perform(get("/api/v1/exam-types/trending")).andExpect(status().isOk()).andExpect(jsonPath("$[0].min_price").exists());

        // RF08 · favoritar, listar e desfavoritar
        mvc.perform(put("/api/v1/me/favorites/{id}", examTypeId).header("Authorization", bearer)).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/me/favorites").header("Authorization", bearer))
                .andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.data[0].exam_type_id").value(examTypeId));
        mvc.perform(delete("/api/v1/me/favorites/{id}", examTypeId).header("Authorization", bearer)).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/me/favorites").header("Authorization", bearer)).andExpect(jsonPath("$.total").value(0));

        // RF09 + RN27 · avaliar a clínica e ver a nota recalculada
        double ratingBefore = jdbc.queryForObject("SELECT rating FROM clinics WHERE id = ?::uuid", Double.class, clinicId);
        int countBefore = jdbc.queryForObject("SELECT review_count FROM clinics WHERE id = ?::uuid", Integer.class, clinicId);
        mvc.perform(put("/api/v1/clinics/{id}/reviews/me", clinicId).header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"rating\":1,\"comment\":\"Demorou.\"}"))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/v1/clinics/{id}", clinicId)).andExpect(jsonPath("$.review_count").value(countBefore + 1));
        double ratingAfter = jdbc.queryForObject("SELECT rating FROM clinics WHERE id = ?::uuid", Double.class, clinicId);
        assertThat(ratingAfter).isLessThan(ratingBefore);
        mvc.perform(get("/api/v1/clinics/{id}/reviews/me", clinicId).header("Authorization", bearer))
                .andExpect(jsonPath("$.rating").value(1));
        // reavaliar substitui (RN25) e não aumenta a contagem
        mvc.perform(put("/api/v1/clinics/{id}/reviews/me", clinicId).header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"rating\":5}")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/clinics/{id}", clinicId)).andExpect(jsonPath("$.review_count").value(countBefore + 1));

        // RF10 · contato por WhatsApp devolve o link com mensagem pronta
        JsonNode contact = json(mvc.perform(post("/api/v1/exams/{id}/contacts", examId).header("Authorization", bearer))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        String link = contact.get("whatsapp_url").asString();
        assertThat(link).startsWith("https://wa.me/55").contains("text=");

        // RF11 · total economizado reflete o contato
        mvc.perform(get("/api/v1/me/savings").header("Authorization", bearer))
                .andExpect(jsonPath("$.total_savings").value(28.00)).andExpect(jsonPath("$.contacts_count").value(1));

        // RF12 · consultar e editar o perfil, trocar a senha e atualizar a foto
        mvc.perform(get("/api/v1/me").header("Authorization", bearer)).andExpect(jsonPath("$.full_name").value("Maria da Silva"))
                .andExpect(jsonPath("$.cpf").exists());
        mvc.perform(put("/api/v1/me").header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"full_name\":\"Maria de Souza\",\"email\":\"maria.souza@email.com\",\"birth_date\":\"1992-03-10\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.full_name").value("Maria de Souza"))
                .andExpect(jsonPath("$.phone").value(phone));
        mvc.perform(multipart("/api/v1/me/avatar").file(new MockMultipartFile("file", "foto.png", "image/png", PNG))
                        .with(r -> { r.setMethod("PUT"); return r; }).header("Authorization", bearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.avatar_url").isNotEmpty());
        mvc.perform(put("/api/v1/me/password").header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"current_password\":\"Senha1234\",\"new_password\":\"NovaSenha99\"}"))
                .andExpect(status().isNoContent());

        // a senha antiga deixa de funcionar e a nova autentica
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"%s\",\"password\":\"Senha1234\"}".formatted(phone))).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"%s\",\"password\":\"NovaSenha99\"}".formatted(phone))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Visitante · busca, compara e abre o WhatsApp sem conta; o contato fica sem usuário (RN29)")
    void visitorJourney() throws Exception {
        JsonNode search = json(mvc.perform(get("/api/v1/exams/search").param("query", "colesterol"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        String examId = search.get("data").get(0).get("id").asString();

        mvc.perform(get("/api/v1/exams/{id}", examId)).andExpect(status().isOk());
        mvc.perform(post("/api/v1/exams/{id}/contacts", examId)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.whatsapp_url").exists());

        Integer withoutUser = jdbc.queryForObject(
                "SELECT count(*) FROM contact_events WHERE exam_id = ?::uuid AND user_id IS NULL AND created_at > now() - interval '1 minute'",
                Integer.class, examId);
        assertThat(withoutUser).isEqualTo(1);
    }

    @Test
    @DisplayName("RN28 · mensagem do WhatsApp cita o exame e o preço no formato do documento")
    void whatsappMessageFollowsTheTemplate() throws Exception {
        UUID exam = jdbc.queryForObject("""
                SELECT e.id FROM exams e JOIN clinics c ON c.id = e.clinic_id JOIN exam_types t ON t.id = e.exam_type_id
                WHERE c.name = 'Clínica Santa Mariazinha' AND t.name = 'Ultrassom Abdominal Total'""", UUID.class);

        String link = json(mvc.perform(post("/api/v1/exams/{id}/contacts", exam)).andReturn().getResponse().getContentAsString())
                .get("whatsapp_url").asString();
        String decoded = java.net.URLDecoder.decode(link.substring(link.indexOf("text=") + 5), java.nio.charset.StandardCharsets.UTF_8);

        assertThat(decoded).isEqualTo(
                "Olá! Vi o exame Ultrassom Abdominal Total por R$ 90,00 no Clinics e gostaria de agendar.");
    }
}
