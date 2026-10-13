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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class FavoritesIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper mapper;

    private UUID type(String name) {
        return jdbc.queryForObject("SELECT id FROM exam_types WHERE name = ?", UUID.class, name);
    }

    @Test
    void favoritesRequireAuthentication() throws Exception {
        UUID id = type("Hemograma Completo");
        mvc.perform(put("/api/v1/me/favorites/{id}", id)).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/v1/me/favorites/{id}", id)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/me/favorites")).andExpect(status().isUnauthorized());
    }

    @Test
    void favoritedExamAppearsWithItsLowestCurrentPrice() throws Exception {
        TestUser user = TestUsers.register(mvc, mapper);
        UUID hemograma = type("Hemograma Completo");

        mvc.perform(put("/api/v1/me/favorites/{id}", hemograma).header("Authorization", user.bearer()))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/me/favorites").header("Authorization", user.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.data[0].exam_type_id").value(hemograma.toString()))
                .andExpect(jsonPath("$.data[0].name").value("Hemograma Completo"))
                .andExpect(jsonPath("$.data[0].category").value("Sangue"))
                .andExpect(jsonPath("$.data[0].min_price").value(18.00))
                .andExpect(jsonPath("$.data[0].offers_count").value(3));
    }

    @Test
    void favoritingTwiceIsIdempotent() throws Exception {
        TestUser user = TestUsers.register(mvc, mapper);
        UUID id = type("Glicemia de Jejum");

        mvc.perform(put("/api/v1/me/favorites/{id}", id).header("Authorization", user.bearer())).andExpect(status().isNoContent());
        mvc.perform(put("/api/v1/me/favorites/{id}", id).header("Authorization", user.bearer())).andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/me/favorites").header("Authorization", user.bearer()))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void removingIsIdempotentToo() throws Exception {
        TestUser user = TestUsers.register(mvc, mapper);
        UUID id = type("Glicemia de Jejum");
        mvc.perform(put("/api/v1/me/favorites/{id}", id).header("Authorization", user.bearer()));

        mvc.perform(delete("/api/v1/me/favorites/{id}", id).header("Authorization", user.bearer())).andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/me/favorites/{id}", id).header("Authorization", user.bearer())).andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/me/favorites").header("Authorization", user.bearer()))
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void unknownExamTypeIs404() throws Exception {
        TestUser user = TestUsers.register(mvc, mapper);
        mvc.perform(put("/api/v1/me/favorites/{id}", UUID.randomUUID()).header("Authorization", user.bearer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EXAM_TYPE_NOT_FOUND"));
    }

    @Test
    void eachUserOnlySeesTheirOwnFavorites() throws Exception {
        TestUser a = TestUsers.register(mvc, mapper);
        TestUser b = TestUsers.register(mvc, mapper);
        mvc.perform(put("/api/v1/me/favorites/{id}", type("Hemograma Completo")).header("Authorization", a.bearer()));

        mvc.perform(get("/api/v1/me/favorites").header("Authorization", a.bearer())).andExpect(jsonPath("$.total").value(1));
        mvc.perform(get("/api/v1/me/favorites").header("Authorization", b.bearer())).andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void listsNewestFirstAndPaginates() throws Exception {
        TestUser user = TestUsers.register(mvc, mapper);
        for (String name : new String[]{"Hemograma Completo", "Glicemia de Jejum", "Lipidograma Completo"}) {
            mvc.perform(put("/api/v1/me/favorites/{id}", type(name)).header("Authorization", user.bearer()));
            Thread.sleep(5);
        }

        mvc.perform(get("/api/v1/me/favorites").param("size", "2").header("Authorization", user.bearer()))
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.total_pages").value(2))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name").value("Lipidograma Completo"))
                .andExpect(jsonPath("$.data[1].name").value("Glicemia de Jejum"));
        mvc.perform(get("/api/v1/me/favorites").param("size", "2").param("page", "1").header("Authorization", user.bearer()))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Hemograma Completo"));
    }
}
