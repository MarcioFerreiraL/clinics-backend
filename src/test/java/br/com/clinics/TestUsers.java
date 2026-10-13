package br.com.clinics;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Cria pacientes reais pela API, com telefone aleatório, para os testes que exigem autenticação. */
public final class TestUsers {

    public static final String PASSWORD = "Senha1234";

    private TestUsers() {
    }

    public record TestUser(UUID id, String phone, String token) {

        public String bearer() {
            return "Bearer " + token;
        }
    }

    public static TestUser register(MockMvc mvc, ObjectMapper mapper) throws Exception {
        String phone = "(81) 9%04d-%04d".formatted(ThreadLocalRandom.current().nextInt(10000), ThreadLocalRandom.current().nextInt(10000));
        String json = mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"full_name\":\"Maria Silva\",\"phone\":\"%s\",\"password\":\"%s\"}".formatted(phone, PASSWORD)))
                .andReturn().getResponse().getContentAsString();
        JsonNode node = mapper.readTree(json);
        return new TestUser(UUID.fromString(node.get("user").get("id").asString()), phone, node.get("token").asString());
    }
}
