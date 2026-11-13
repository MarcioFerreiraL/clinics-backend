package br.com.clinics.engagement;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import br.com.clinics.IntegrationTest;
import br.com.clinics.TestUsers;
import br.com.clinics.TestUsers.TestUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/** Toques duplos no app chegam ao mesmo tempo: nenhum deles pode virar erro 500 nem deixar a nota errada. */
@IntegrationTest
class ConcurrentWritesIntegrationTest {

    private static final int REQUESTS = 8;
    private static final int ROUNDS = 5;

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

    /** Dispara as requisições juntas e devolve o status de cada uma. */
    private List<Integer> together(List<MockHttpServletRequestBuilder> requests) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService pool = Executors.newFixedThreadPool(requests.size())) {
            List<Future<Integer>> results = new ArrayList<>();
            for (MockHttpServletRequestBuilder request : requests) {
                Callable<Integer> call = () -> {
                    start.await();
                    return mvc.perform(request).andReturn().getResponse().getStatus();
                };
                results.add(pool.submit(call));
            }
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> result : results) {
                statuses.add(result.get());
            }
            return statuses;
        }
    }

    private MockHttpServletRequestBuilder reviewRequest(TestUser user, UUID clinic, int rating) {
        return put("/api/v1/clinics/{id}/reviews/me", clinic).header("Authorization", user.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{\"rating\":%d}".formatted(rating));
    }

    @Test
    void sameReviewSentTogetherKeepsOneRow() throws Exception {
        for (int round = 0; round < ROUNDS; round++) {
            UUID clinic = newClinic();
            TestUser user = TestUsers.register(mvc, mapper);
            List<MockHttpServletRequestBuilder> requests = new ArrayList<>();
            for (int i = 0; i < REQUESTS; i++) {
                requests.add(reviewRequest(user, clinic, 4));
            }

            assertThat(together(requests)).allMatch(s -> s == 200 || s == 201);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM reviews WHERE clinic_id = ?", Long.class, clinic))
                    .isEqualTo(1);
        }
    }

    @Test
    void reviewsFromDifferentPatientsAllCountInTheClinicRating() throws Exception {
        for (int round = 0; round < ROUNDS; round++) {
            UUID clinic = newClinic();
            List<MockHttpServletRequestBuilder> requests = new ArrayList<>();
            for (int i = 0; i < REQUESTS; i++) {
                requests.add(reviewRequest(TestUsers.register(mvc, mapper), clinic, 5));
            }

            assertThat(together(requests)).allMatch(s -> s == 201);
            assertThat(jdbc.queryForObject("SELECT review_count FROM clinics WHERE id = ?", Integer.class, clinic))
                    .isEqualTo(REQUESTS);
        }
    }

    @Test
    void favoritingAndRemovingTogetherHaveNoEffectBeyondTheFirst() throws Exception {
        UUID examType = jdbc.queryForObject("SELECT id FROM exam_types LIMIT 1", UUID.class);
        for (int round = 0; round < ROUNDS; round++) {
            TestUser user = TestUsers.register(mvc, mapper);
            List<MockHttpServletRequestBuilder> adds = new ArrayList<>();
            List<MockHttpServletRequestBuilder> removes = new ArrayList<>();
            for (int i = 0; i < REQUESTS; i++) {
                adds.add(put("/api/v1/me/favorites/{id}", examType).header("Authorization", user.bearer()));
                removes.add(delete("/api/v1/me/favorites/{id}", examType).header("Authorization", user.bearer()));
            }

            assertThat(together(adds)).allMatch(s -> s < 300);
            assertThat(together(removes)).allMatch(s -> s < 300);
        }
    }
}
