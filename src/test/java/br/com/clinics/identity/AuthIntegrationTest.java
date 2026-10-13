package br.com.clinics.identity;

import java.util.concurrent.ThreadLocalRandom;

import br.com.clinics.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class AuthIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtDecoder jwtDecoder;

    private static String randomPhone() {
        return "(81) 9%04d-%04d".formatted(ThreadLocalRandom.current().nextInt(10000), ThreadLocalRandom.current().nextInt(10000));
    }

    private String body(Object... pairs) {
        var node = mapper.createObjectNode();
        for (int i = 0; i < pairs.length; i += 2) {
            node.put((String) pairs[i], (String) pairs[i + 1]);
        }
        return node.toString();
    }

    private MvcResult register(String phone, String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(body("full_name", "Maria Silva", "phone", phone, "password", password))).andReturn();
    }

    @Test
    void registersPatientAndReturnsUsableToken() throws Exception {
        String phone = randomPhone();

        MvcResult result = mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(body("full_name", "Maria Silva", "phone", phone, "password", "Senha1234")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.user.role").value("PATIENT"))
                .andExpect(jsonPath("$.user.phone").value(phone))
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andExpect(jsonPath("$.user.password_hash").doesNotExist())
                .andReturn();

        JsonNode json = mapper.readTree(result.getResponse().getContentAsString());
        Jwt jwt = jwtDecoder.decode(json.get("token").asString());
        assertThat(jwt.getSubject()).isEqualTo(json.get("user").get("id").asString());
        assertThat(jwt.<String>getClaim("role")).isEqualTo("PATIENT");
        assertThat(jwt.getExpiresAt()).isNotNull();
    }

    @Test
    void storesPasswordAsBcryptHash() throws Exception {
        String phone = randomPhone();
        register(phone, "Senha1234");

        String stored = jdbc.queryForObject("SELECT password_hash FROM users WHERE phone = ?", String.class,
                "55" + phone.replaceAll("\\D", ""));
        assertThat(stored).startsWith("$2").doesNotContain("Senha1234");
    }

    @Test
    void rejectsDuplicatedPhone() throws Exception {
        String phone = randomPhone();
        register(phone, "Senha1234");

        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(body("full_name", "Outra Pessoa", "phone", phone, "password", "Senha1234")))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("PHONE_ALREADY_REGISTERED"));
    }

    @Test
    void rejectsWeakPassword() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(body("full_name", "Maria", "phone", randomPhone(), "password", "curta1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WEAK_PASSWORD"));
    }

    @Test
    void rejectsInvalidPhone() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(body("full_name", "Maria", "phone", "(81) 3634-1122", "password", "Senha1234")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PHONE"));
    }

    @Test
    void reportsMissingFieldsWithFieldNames() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.length()").value(3))
                .andExpect(jsonPath("$.errors[*].field").value(org.hamcrest.Matchers.containsInAnyOrder("full_name", "phone", "password")));
    }

    @Test
    void logsInWithCorrectCredentials() throws Exception {
        String phone = randomPhone();
        register(phone, "Senha1234");

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body("phone", phone, "password", "Senha1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.role").value("PATIENT"));
    }

    @Test
    void logsInWithUnformattedPhone() throws Exception {
        String phone = randomPhone();
        register(phone, "Senha1234");

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body("phone", phone.replaceAll("\\D", ""), "password", "Senha1234")))
                .andExpect(status().isOk());
    }

    @Test
    void failedLoginIsIndistinguishableForWrongPasswordAndUnknownPhone() throws Exception {
        String phone = randomPhone();
        register(phone, "Senha1234");

        String wrongPassword = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body("phone", phone, "password", "SenhaErrada9")))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        String unknownPhone = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body("phone", randomPhone(), "password", "Senha1234")))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertThat(wrongPassword).isEqualTo(unknownPhone);
        assertThat(wrongPassword).contains("INVALID_CREDENTIALS");
    }

    @Test
    void protectedRoutesRequireAuthentication() throws Exception {
        mvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }
}
