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

    /** CPF válido e aleatório (dígitos verificadores calculados). */
    public static String randomCpf() {
        int[] d = new int[11];
        do {
            for (int i = 0; i < 9; i++) {
                d[i] = ThreadLocalRandom.current().nextInt(10);
            }
        } while (java.util.Arrays.stream(d, 0, 9).distinct().count() == 1);
        for (int n = 9; n < 11; n++) {
            int sum = 0;
            for (int i = 0; i < n; i++) {
                sum += d[i] * (n + 1 - i);
            }
            int rest = (sum * 10) % 11;
            d[n] = rest == 10 ? 0 : rest;
        }
        StringBuilder sb = new StringBuilder();
        for (int x : d) {
            sb.append(x);
        }
        return sb.toString();
    }

    public static String randomEmail() {
        return "paciente" + UUID.randomUUID().toString().substring(0, 8) + "@email.com";
    }

    /** Corpo completo de cadastro, com CPF, e-mail e nascimento válidos e únicos. */
    public static String registrationJson(String name, String phone, String password) {
        return "{\"full_name\":\"%s\",\"phone\":\"%s\",\"cpf\":\"%s\",\"email\":\"%s\",\"birth_date\":\"1990-05-20\",\"password\":\"%s\"}"
                .formatted(name, phone, randomCpf(), randomEmail(), password);
    }

    public static TestUser register(MockMvc mvc, ObjectMapper mapper) throws Exception {
        String phone = "(81) 9%04d-%04d".formatted(ThreadLocalRandom.current().nextInt(10000), ThreadLocalRandom.current().nextInt(10000));
        String json = mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson("Maria Silva", phone, PASSWORD)))
                .andReturn().getResponse().getContentAsString();
        JsonNode node = mapper.readTree(json);
        return new TestUser(UUID.fromString(node.get("user").get("id").asString()), phone, node.get("token").asString());
    }
}
