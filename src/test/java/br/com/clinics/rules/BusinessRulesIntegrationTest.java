package br.com.clinics.rules;

import java.util.UUID;

import br.com.clinics.IntegrationTest;
import br.com.clinics.TestUsers;
import br.com.clinics.TestUsers.TestUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regras de negócio (docs/03-regras-de-negocio.md) verificadas pela API. A classe é transacional:
 * tudo que o teste grava é desfeito no fim, então não interfere nos outros testes.
 */
@IntegrationTest
@Transactional
class BusinessRulesIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper mapper;
    @Autowired JwtDecoder jwtDecoder;

    private String phone() {
        return "(81) 9%04d-%04d".formatted((int) (Math.random() * 10000), (int) (Math.random() * 10000));
    }

    private UUID clinicId(String name) {
        return jdbc.queryForObject("SELECT id FROM clinics WHERE name = ?", UUID.class, name);
    }

    // ---------- conta e autenticação ----------

    @Test
    @DisplayName("RN05 · o cadastro ignora tentativa de escolher papel privilegiado")
    void registrationAlwaysCreatesPatient() throws Exception {
        JsonNode body = mapper.readTree(TestUsers.registrationJson("Maria", phone(), "Senha1234"));
        ((tools.jackson.databind.node.ObjectNode) body).put("role", "ADMIN");

        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("PATIENT"));
    }

    @Test
    @DisplayName("RN06 · login falho não revela se o telefone existe")
    void loginFailureIsIdenticalForUnknownPhoneAndWrongPassword() throws Exception {
        String known = phone();
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(TestUsers.registrationJson("Maria", known, "Senha1234"))).andExpect(status().isCreated());

        String wrongPassword = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"%s\",\"password\":\"Errada123\"}".formatted(known)))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        String unknownPhone = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"(81) 90000-0000\",\"password\":\"Senha1234\"}"))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();

        JsonNode a = mapper.readTree(wrongPassword);
        JsonNode b = mapper.readTree(unknownPhone);
        assertThat(a.get("code").asString()).isEqualTo("INVALID_CREDENTIALS").isEqualTo(b.get("code").asString());
        assertThat(a.get("detail").asString()).isEqualTo(b.get("detail").asString());
    }

    @Test
    @DisplayName("RN07 · o token vale 1 hora e traz sub, role e exp")
    void tokenLastsOneHour() throws Exception {
        TestUser user = TestUsers.register(mvc, mapper);
        Jwt jwt = jwtDecoder.decode(user.token());

        assertThat(jwt.getSubject()).isEqualTo(user.id().toString());
        assertThat(jwt.<String>getClaim("role")).isEqualTo("PATIENT");
        assertThat(jwt.getExpiresAt().getEpochSecond() - jwt.getIssuedAt().getEpochSecond()).isEqualTo(3600);
    }

    @Test
    @DisplayName("RN36 · respostas nunca expõem senha nem hash")
    void responsesNeverLeakSecrets() throws Exception {
        TestUser user = TestUsers.register(mvc, mapper);
        String me = mvc.perform(get("/api/v1/me").header("Authorization", user.bearer()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertThat(me).doesNotContainIgnoringCase("password").doesNotContain("$2");
    }

    // ---------- autorização ----------

    @Test
    @DisplayName("RN24 / RN35 · rotas do paciente exigem autenticação")
    void patientRoutesRequireAuthentication() throws Exception {
        UUID clinic = clinicId("Clínica Santa Mariazinha");
        mvc.perform(get("/api/v1/me")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/v1/me").contentType(MediaType.APPLICATION_JSON).content("{\"full_name\":\"X\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(put("/api/v1/me/password").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/me/savings")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/me/favorites")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/clinics/{id}/reviews/me", clinic)).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/v1/clinics/{id}/reviews/me", clinic).contentType(MediaType.APPLICATION_JSON)
                .content("{\"rating\":5}")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("RN24 · busca, detalhes, clínica, bairros e em alta são públicos")
    void catalogRoutesArePublic() throws Exception {
        mvc.perform(get("/api/v1/exams/search")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/exam-types/trending")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/neighborhoods")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/clinics/{id}", clinicId("Clínica Santa Mariazinha"))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/clinics/{id}/reviews", clinicId("Clínica Santa Mariazinha"))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("RN35 · um paciente nunca vê os favoritos de outro")
    void favoritesAreIsolatedPerUser() throws Exception {
        TestUser a = TestUsers.register(mvc, mapper);
        TestUser b = TestUsers.register(mvc, mapper);
        UUID type = jdbc.queryForObject("SELECT id FROM exam_types WHERE name = 'Hemograma Completo'", UUID.class);

        mvc.perform(put("/api/v1/me/favorites/{id}", type).header("Authorization", a.bearer()))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/me/favorites").header("Authorization", b.bearer()))
                .andExpect(jsonPath("$.total").value(0));
    }

    // ---------- catálogo e busca ----------

    @Test
    @DisplayName("RN12 · oferta indisponível não aparece na busca")
    void unavailableOffersAreHidden() throws Exception {
        jdbc.update("""
                UPDATE exams SET available = FALSE WHERE id = (
                  SELECT e.id FROM exams e JOIN clinics c ON c.id = e.clinic_id JOIN exam_types t ON t.id = e.exam_type_id
                  WHERE c.name = 'Clínica Santa Mariazinha' AND t.name = 'Ultrassom Abdominal Total')""");

        mvc.perform(get("/api/v1/exams/search").param("query", "ultrassom abdominal"))
                .andExpect(jsonPath("$.total").value(4))
                .andExpect(jsonPath("$.data[*].clinic.name").value(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.hasItem("Clínica Santa Mariazinha"))));
    }

    @Test
    @DisplayName("RN15 · a economia de qualquer oferta nunca é negativa")
    void savingsAreNeverNegative() throws Exception {
        mvc.perform(get("/api/v1/exams/search").param("query", "ultrassom abdominal").param("size", "50"))
                .andExpect(jsonPath("$.data[*].savings").value(everyItem(greaterThanOrEqualTo(0.0))));
    }

    @Test
    @DisplayName("RN15 · em empate de menor preço, todas as ofertas empatadas levam o selo")
    void tiedCheapestOffersAllGetTheBestPriceBadge() throws Exception {
        jdbc.update("""
                UPDATE exams SET price = 90.00 WHERE id = (
                  SELECT e.id FROM exams e JOIN clinics c ON c.id = e.clinic_id JOIN exam_types t ON t.id = e.exam_type_id
                  WHERE c.name = 'Policlínica do Agrestão' AND t.name = 'Ultrassom Abdominal Total')""");

        mvc.perform(get("/api/v1/exams/search").param("query", "ultrassom abdominal"))
                .andExpect(jsonPath("$.data[?(@.best_price == true)]", hasSize(2)));
    }

    @Test
    @DisplayName("RN17 / RN19 · ordenar por distância devolve do mais perto para o mais longe")
    void distanceSortIsAscending() throws Exception {
        mvc.perform(get("/api/v1/exams/search").param("query", "hemograma").param("sort_by", "distance")
                        .param("lat", "-7.8335").param("lng", "-35.7530"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].distance_km").exists());

        String json = mvc.perform(get("/api/v1/exams/search").param("query", "hemograma").param("sort_by", "distance")
                        .param("lat", "-7.8335").param("lng", "-35.7530"))
                .andReturn().getResponse().getContentAsString();
        double previous = -1;
        for (JsonNode item : mapper.readTree(json).get("data")) {
            double d = item.get("distance_km").asDouble();
            assertThat(d).isGreaterThanOrEqualTo(previous);
            previous = d;
        }
    }

    @Test
    @DisplayName("RN20 / RNF09 · buscar com localização não grava nada no banco")
    void searchingWithLocationStoresNothing() throws Exception {
        int before = jdbc.queryForObject("SELECT (SELECT count(*) FROM users) + (SELECT count(*) FROM contact_events)"
                + " + (SELECT count(*) FROM favorites) + (SELECT count(*) FROM reviews)", Integer.class);

        mvc.perform(get("/api/v1/exams/search").param("sort_by", "distance").param("lat", "-7.8335").param("lng", "-35.7530"))
                .andExpect(status().isOk());

        int after = jdbc.queryForObject("SELECT (SELECT count(*) FROM users) + (SELECT count(*) FROM contact_events)"
                + " + (SELECT count(*) FROM favorites) + (SELECT count(*) FROM reviews)", Integer.class);
        assertThat(after).isEqualTo(before);
    }

    @Test
    @DisplayName("RN21 / RNF04 · toda listagem pagina com tamanho máximo de 50")
    void listingsCapPageSize() throws Exception {
        TestUser user = TestUsers.register(mvc, mapper);
        mvc.perform(get("/api/v1/exams/search").param("size", "999")).andExpect(jsonPath("$.size").value(50));
        mvc.perform(get("/api/v1/clinics/{id}/reviews", clinicId("Clínica Santa Mariazinha")).param("size", "999"))
                .andExpect(jsonPath("$.size").value(50));
        mvc.perform(get("/api/v1/me/favorites").param("size", "999").header("Authorization", user.bearer()))
                .andExpect(jsonPath("$.size").value(50));
        mvc.perform(get("/api/v1/exams/search")).andExpect(jsonPath("$.size").value(20));
    }

    @Test
    @DisplayName("RN38 · filtros de bairro, preço e nota se combinam")
    void filtersCombine() throws Exception {
        mvc.perform(get("/api/v1/exams/search").param("neighborhood", "Centro").param("max_price", "60")
                        .param("min_rating", "4.5").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].clinic.neighborhood").value(everyItem(org.hamcrest.Matchers.is("Centro"))))
                .andExpect(jsonPath("$.data[*].price").value(everyItem(lessThanOrEqualTo(60.0))))
                .andExpect(jsonPath("$.data[*].clinic.rating").value(everyItem(greaterThanOrEqualTo(4.5))));
    }

    @Test
    @DisplayName("RN38 · os bairros são listados em ordem alfabética sem repetição")
    void neighborhoodsAreSortedAndDistinct() throws Exception {
        String json = mvc.perform(get("/api/v1/neighborhoods")).andReturn().getResponse().getContentAsString();
        java.util.List<String> names = new java.util.ArrayList<>();
        mapper.readTree(json).forEach(n -> names.add(n.get("name").asString()));
        assertThat(names).doesNotHaveDuplicates().isSortedAccordingTo(java.text.Collator.getInstance(java.util.Locale.forLanguageTag("pt-BR")));
    }

    @Test
    @DisplayName("RN30 · Em alta devolve até 5 exames, do mais contatado para o menos")
    void trendingIsCappedAndOrdered() throws Exception {
        String json = mvc.perform(get("/api/v1/exam-types/trending")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode items = mapper.readTree(json);
        assertThat(items.size()).isBetween(1, 5);
        long previous = Long.MAX_VALUE;
        for (JsonNode item : items) {
            long contacts = item.get("recent_contacts").asLong();
            assertThat(contacts).isLessThanOrEqualTo(previous);
            previous = contacts;
        }
    }

    // ---------- erros padronizados ----------

    @Test
    @DisplayName("RNF07 · erro de oferta inexistente segue a RFC 9457 com campo code")
    void notFoundFollowsProblemDetails() throws Exception {
        mvc.perform(get("/api/v1/exams/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("EXAM_NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.instance").exists());
    }

    @Test
    @DisplayName("RNF07 · erros de validação listam os campos em snake_case")
    void validationErrorsListFieldsInSnakeCase() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[*].field").value(org.hamcrest.Matchers.hasItems("full_name", "birth_date")));
    }

    @Test
    @DisplayName("RNF07 · corpo malformado e id inválido viram erro 400 padronizado, sem detalhes técnicos")
    void malformedInputIsABadRequest() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{nao-e-json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Exception"))));
        mvc.perform(get("/api/v1/exams/{id}", "isto-nao-e-uuid")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("RNF06 · o contrato OpenAPI documenta busca, bairros e cadastro")
    void openApiDocumentsTheMainEndpoints() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/exams/search']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/neighborhoods']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/register']").exists());
    }
}
