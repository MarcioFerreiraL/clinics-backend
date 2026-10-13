package br.com.clinics.identity;

import java.nio.file.Files;
import java.nio.file.Path;

import br.com.clinics.IntegrationTest;
import br.com.clinics.TestUsers;
import br.com.clinics.TestUsers.TestUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class ProfileIntegrationTest {

    private static final Path AVATARS = Path.of("target/test-avatars").toAbsolutePath();

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    private static byte[] png(int size) {
        byte[] data = new byte[size];
        data[0] = (byte) 0x89;
        data[1] = 'P';
        data[2] = 'N';
        data[3] = 'G';
        return data;
    }

    private MockMultipartHttpServletRequestBuilder avatarRequest(byte[] bytes) {
        MockMultipartHttpServletRequestBuilder request = multipart("/api/v1/me/avatar").file(
                new MockMultipartFile("file", "foto.png", MediaType.IMAGE_PNG_VALUE, bytes));
        request.with(r -> {
            r.setMethod("PUT");
            return r;
        });
        return request;
    }

    // ---------- UC11 · perfil ----------

    @Test
    void profileRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/v1/me")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/v1/me").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
    }

    @Test
    void returnsOwnProfileWithoutSensitiveData() throws Exception {
        TestUser user = TestUsers.register(mvc, mapper);

        mvc.perform(get("/api/v1/me").header("Authorization", user.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.id().toString()))
                .andExpect(jsonPath("$.full_name").value("Maria Silva"))
                .andExpect(jsonPath("$.phone").value(user.phone()))
                .andExpect(jsonPath("$.role").value("PATIENT"))
                .andExpect(jsonPath("$.password_hash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void updatesEditableFieldsAndIgnoresPhone() throws Exception {
        TestUser user = TestUsers.register(mvc, mapper);

        mvc.perform(put("/api/v1/me").header("Authorization", user.bearer()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"full_name\":\"Maria de Souza\",\"email\":\"Maria@Email.com\",\"birth_date\":\"1996-08-14\",\"phone\":\"(81) 90000-0000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.full_name").value("Maria de Souza"))
                .andExpect(jsonPath("$.email").value("maria@email.com"))
                .andExpect(jsonPath("$.birth_date").value("1996-08-14"))
                .andExpect(jsonPath("$.phone").value(user.phone()));

        mvc.perform(get("/api/v1/me").header("Authorization", user.bearer()))
                .andExpect(jsonPath("$.full_name").value("Maria de Souza"));
    }

    @Test
    void rejectsInvalidProfileData() throws Exception {
        TestUser user = TestUsers.register(mvc, mapper);

        mvc.perform(put("/api/v1/me").header("Authorization", user.bearer()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"full_name\":\"Maria\",\"email\":\"sem-arroba\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_EMAIL"));
        mvc.perform(put("/api/v1/me").header("Authorization", user.bearer()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"full_name\":\"Maria\",\"birth_date\":\"2999-01-01\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_BIRTH_DATE"));
        mvc.perform(put("/api/v1/me").header("Authorization", user.bearer()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"full_name\":\"\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void rejectsEmailUsedByAnotherAccountButAllowsKeepingYourOwn() throws Exception {
        TestUser first = TestUsers.register(mvc, mapper);
        TestUser second = TestUsers.register(mvc, mapper);
        String email = "unico-%s@email.com".formatted(first.id());
        String body = "{\"full_name\":\"Maria\",\"email\":\"%s\"}".formatted(email);
        String sameEmailInUpperCase = "{\"full_name\":\"Maria\",\"email\":\"%s\"}".formatted(email.toUpperCase());

        mvc.perform(put("/api/v1/me").header("Authorization", first.bearer()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        mvc.perform(put("/api/v1/me").header("Authorization", first.bearer()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        mvc.perform(put("/api/v1/me").header("Authorization", second.bearer()).contentType(MediaType.APPLICATION_JSON).content(sameEmailInUpperCase))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("EMAIL_ALREADY_IN_USE"));
    }

    @Test
    void usersOnlyEverSeeTheirOwnData() throws Exception {
        TestUser a = TestUsers.register(mvc, mapper);
        TestUser b = TestUsers.register(mvc, mapper);

        mvc.perform(get("/api/v1/me").header("Authorization", a.bearer())).andExpect(jsonPath("$.id").value(a.id().toString()));
        mvc.perform(get("/api/v1/me").header("Authorization", b.bearer())).andExpect(jsonPath("$.id").value(b.id().toString()));
    }

    // ---------- UC11 · avatar ----------

    @Test
    void uploadsAvatarAndServesTheFile() throws Exception {
        TestUser user = TestUsers.register(mvc, mapper);

        String json = mvc.perform(avatarRequest(png(500)).header("Authorization", user.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatar_url").value(org.hamcrest.Matchers.startsWith("/files/avatars/" + user.id())))
                .andReturn().getResponse().getContentAsString();

        String url = mapper.readTree(json).get("avatar_url").asString();
        mvc.perform(get(url)).andExpect(status().isOk()).andExpect(content().bytes(png(500)));
    }

    @Test
    void replacingTheAvatarRemovesTheOldFile() throws Exception {
        TestUser user = TestUsers.register(mvc, mapper);
        String first = mapper.readTree(mvc.perform(avatarRequest(png(100)).header("Authorization", user.bearer()))
                .andReturn().getResponse().getContentAsString()).get("avatar_url").asString();
        Path firstFile = AVATARS.resolve(first.substring("/files/avatars/".length()));
        assertThat(firstFile).exists();

        mvc.perform(avatarRequest(png(200)).header("Authorization", user.bearer())).andExpect(status().isOk());

        assertThat(Files.exists(firstFile)).isFalse();
    }

    @Test
    void rejectsNonImageAndOversizedAvatars() throws Exception {
        TestUser user = TestUsers.register(mvc, mapper);

        mvc.perform(avatarRequest("isto não é uma imagem".getBytes()).header("Authorization", user.bearer()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_AVATAR"));
        mvc.perform(avatarRequest(png(2 * 1024 * 1024 + 1)).header("Authorization", user.bearer()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_AVATAR"));
    }

    @Test
    void avatarUploadRequiresAuthentication() throws Exception {
        mvc.perform(avatarRequest(png(10))).andExpect(status().isUnauthorized());
    }

    // ---------- UC12 · senha ----------

    private String passwordBody(String current, String next) {
        return "{\"current_password\":\"%s\",\"new_password\":\"%s\"}".formatted(current, next);
    }

    @Test
    void changesPasswordAndOldOneStopsWorking() throws Exception {
        TestUser user = TestUsers.register(mvc, mapper);

        mvc.perform(put("/api/v1/me/password").header("Authorization", user.bearer()).contentType(MediaType.APPLICATION_JSON)
                        .content(passwordBody(TestUsers.PASSWORD, "NovaSenha99")))
                .andExpect(status().isNoContent());

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"%s\",\"password\":\"NovaSenha99\"}".formatted(user.phone())))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"%s\",\"password\":\"%s\"}".formatted(user.phone(), TestUsers.PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsWrongCurrentPassword() throws Exception {
        TestUser user = TestUsers.register(mvc, mapper);

        mvc.perform(put("/api/v1/me/password").header("Authorization", user.bearer()).contentType(MediaType.APPLICATION_JSON)
                        .content(passwordBody("SenhaErrada1", "NovaSenha99")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_CURRENT_PASSWORD"));

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"%s\",\"password\":\"%s\"}".formatted(user.phone(), TestUsers.PASSWORD)))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsSamePasswordAndWeakPassword() throws Exception {
        TestUser user = TestUsers.register(mvc, mapper);

        mvc.perform(put("/api/v1/me/password").header("Authorization", user.bearer()).contentType(MediaType.APPLICATION_JSON)
                        .content(passwordBody(TestUsers.PASSWORD, TestUsers.PASSWORD)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("SAME_PASSWORD"));
        mvc.perform(put("/api/v1/me/password").header("Authorization", user.bearer()).contentType(MediaType.APPLICATION_JSON)
                        .content(passwordBody(TestUsers.PASSWORD, "fraca")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("WEAK_PASSWORD"));
    }

    @Test
    void passwordChangeRequiresAuthentication() throws Exception {
        mvc.perform(put("/api/v1/me/password").contentType(MediaType.APPLICATION_JSON).content(passwordBody("a", "b")))
                .andExpect(status().isUnauthorized());
    }
}
