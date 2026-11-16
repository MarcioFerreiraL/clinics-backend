package br.com.clinics.access;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import br.com.clinics.IntegrationTest;
import br.com.clinics.TestUsers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

/** RBAC: camada 1 (papel), camada 2 (permissão e escopo), administração da matriz e auditoria. */
@IntegrationTest
@DisplayName("RBAC · papel, permissão, escopo e auditoria")
class RbacIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper mapper;

    private final List<UUID> createdUsers = new ArrayList<>();
    private UUID clinicA;
    private UUID clinicB;

    @BeforeEach
    void clinics() {
        List<UUID> ids = jdbc.queryForList("SELECT id FROM clinics ORDER BY name LIMIT 2", UUID.class);
        clinicA = ids.get(0);
        clinicB = ids.get(1);
    }

    @AfterEach
    void cleanUp() {
        for (UUID id : createdUsers) {
            jdbc.update("DELETE FROM permission_audit_logs WHERE actor_user_id = ? OR target_user_id = ?", id, id);
            jdbc.update("DELETE FROM users WHERE id = ?", id);
        }
        // devolve a matriz ao estado do seed, caso um teste tenha falhado no meio
        jdbc.update("""
                INSERT INTO position_permissions SELECT p.id, m.id FROM positions p, permissions m
                WHERE (p.name = 'Administrador') OR (p.name = 'Atendente' AND m.action = 'ver')
                   OR (p.name = 'Gestor da clínica' AND m.resource IN ('clinica', 'exame', 'indicadores'))
                ON CONFLICT DO NOTHING""");
    }

    /** Cria uma conta com o cargo dado (e a clínica, se o cargo tiver escopo) e devolve o token já com o papel certo. */
    private Staff staff(String position, UUID clinicId) throws Exception {
        TestUsers.TestUser user = TestUsers.register(mvc, mapper);
        createdUsers.add(user.id());
        boolean admin = position.equals("Administrador");
        jdbc.update("UPDATE users SET role = ? WHERE id = ?", admin ? "ADMIN" : "CLINIC_MANAGER", user.id());
        jdbc.update("INSERT INTO internal_users (user_id, position_id, clinic_id) VALUES (?, (SELECT id FROM positions WHERE name = ?), ?)",
                user.id(), position, admin ? null : clinicId);
        String json = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"%s\",\"password\":\"%s\"}".formatted(user.phone(), TestUsers.PASSWORD)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return new Staff(user.id(), "Bearer " + mapper.readTree(json).get("token").asString());
    }

    private record Staff(UUID id, String bearer) {
    }

    private UUID positionId(String name) {
        return jdbc.queryForObject("SELECT id FROM positions WHERE name = ?", UUID.class, name);
    }

    private UUID permissionId(String key) {
        return jdbc.queryForObject("SELECT id FROM permissions WHERE resource || '.' || action = ?", UUID.class, key);
    }

    // ---------- camada 1 ----------

    @Test
    @DisplayName("sem token é 401 e paciente em rota de servidor é 403")
    void layerOneRoles() throws Exception {
        mvc.perform(get("/api/v1/clinic/me/dashboard")).andExpect(status().isUnauthorized());
        TestUsers.TestUser patient = TestUsers.register(mvc, mapper);
        createdUsers.add(patient.id());
        mvc.perform(get("/api/v1/clinic/me/dashboard").header("Authorization", patient.bearer()))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        mvc.perform(get("/api/v1/admin/accounts").header("Authorization", patient.bearer()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("gestor passa na camada 1 de /clinic mas não de /admin")
    void managerCannotUseAdminRoutes() throws Exception {
        Staff manager = staff("Gestor da clínica", clinicA);
        mvc.perform(get("/api/v1/admin/accounts").header("Authorization", manager.bearer())).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/positions").header("Authorization", manager.bearer())).andExpect(status().isForbidden());
    }

    // ---------- camada 2: permissão ----------

    @Test
    @DisplayName("atendente lê o painel e as ofertas, mas não edita nem desativa")
    void attendantIsReadOnly() throws Exception {
        Staff attendant = staff("Atendente", clinicA);
        mvc.perform(get("/api/v1/clinic/me/dashboard").header("Authorization", attendant.bearer())).andExpect(status().isOk());
        mvc.perform(get("/api/v1/clinic/me/exams").header("Authorization", attendant.bearer())).andExpect(status().isOk());

        UUID examTypeId = jdbc.queryForObject("SELECT id FROM exam_types LIMIT 1", UUID.class);
        mvc.perform(post("/api/v1/clinic/me/exams").header("Authorization", attendant.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exam_type_id\":\"%s\",\"price\":10.00}".formatted(examTypeId)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        mvc.perform(delete("/api/v1/clinic/me/exams/" + UUID.randomUUID()).header("Authorization", attendant.bearer()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("/me/access devolve cargo, clínica e permissões do cargo")
    void myAccess() throws Exception {
        Staff attendant = staff("Atendente", clinicA);
        String json = mvc.perform(get("/api/v1/me/access").header("Authorization", attendant.bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.position").value("Atendente"))
                .andReturn().getResponse().getContentAsString();
        List<String> permissions = new ArrayList<>();
        mapper.readTree(json).get("permissions").forEach(n -> permissions.add(n.asString()));
        assertThat(permissions).containsExactly("clinica.ver", "exame.ver", "indicadores.ver");
    }

    // ---------- camada 2: escopo ----------

    @Test
    @DisplayName("gestor vê a própria clínica; a de outra responde 404, e o administrador vê qualquer uma")
    void scope() throws Exception {
        Staff manager = staff("Gestor da clínica", clinicA);
        mvc.perform(get("/api/v1/clinic/" + clinicA + "/dashboard").header("Authorization", manager.bearer()))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/clinic/" + clinicB + "/dashboard").header("Authorization", manager.bearer()))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("CLINIC_NOT_FOUND"));

        Staff admin = staff("Administrador", null);
        mvc.perform(get("/api/v1/clinic/" + clinicB + "/dashboard").header("Authorization", admin.bearer()))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/clinic/me/dashboard").header("Authorization", admin.bearer()))
                .andExpect(status().isNotFound());
    }

    // ---------- administração e auditoria ----------

    @Test
    @DisplayName("mudar a matriz vale na requisição seguinte, com o mesmo token, e fica na auditoria")
    void matrixChangeIsImmediateAndAudited() throws Exception {
        Staff admin = staff("Administrador", null);
        Staff attendant = staff("Atendente", clinicA);
        UUID position = positionId("Atendente");
        UUID examView = permissionId("exame.ver");
        String url = "/api/v1/admin/positions/%s/permissions/%s".formatted(position, examView);

        mvc.perform(get("/api/v1/clinic/me/exams").header("Authorization", attendant.bearer())).andExpect(status().isOk());
        mvc.perform(delete(url).header("Authorization", admin.bearer())).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/clinic/me/exams").header("Authorization", attendant.bearer())).andExpect(status().isForbidden());
        mvc.perform(put(url).header("Authorization", admin.bearer())).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/clinic/me/exams").header("Authorization", attendant.bearer())).andExpect(status().isOk());

        assertThat(jdbc.queryForList("SELECT action FROM permission_audit_logs WHERE actor_user_id = ? ORDER BY created_at",
                String.class, admin.id())).containsExactly("revoke", "grant");
    }

    @Test
    @DisplayName("o administrador não pode tirar de si a permissão de gerenciar permissões")
    void adminKeepsPermissionManagement() throws Exception {
        Staff admin = staff("Administrador", null);
        mvc.perform(delete("/api/v1/admin/positions/%s/permissions/%s".formatted(positionId("Administrador"),
                        permissionId("permissao.gerenciar"))).header("Authorization", admin.bearer()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("LAST_ADMIN_PERMISSION"));
    }

    @Test
    @DisplayName("administrador lista contas e troca o cargo; o papel da conta acompanha")
    void adminChangesPosition() throws Exception {
        Staff admin = staff("Administrador", null);
        Staff attendant = staff("Atendente", clinicA);

        mvc.perform(get("/api/v1/admin/accounts").header("Authorization", admin.bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.id == '%s')].position".formatted(attendant.id())).value("Atendente"));

        String body = "{\"position_id\":\"%s\",\"clinic_id\":\"%s\"}".formatted(positionId("Gestor da clínica"), clinicB);
        mvc.perform(put("/api/v1/admin/accounts/%s/position".formatted(attendant.id())).header("Authorization", admin.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("SELECT clinic_id FROM internal_users WHERE user_id = ?", UUID.class, attendant.id()))
                .isEqualTo(clinicB);

        mvc.perform(put("/api/v1/admin/accounts/%s/position".formatted(admin.id())).header("Authorization", admin.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("CANNOT_CHANGE_OWN_POSITION"));
        mvc.perform(put("/api/v1/admin/accounts/%s/position".formatted(attendant.id())).header("Authorization", admin.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"position_id\":\"%s\"}".formatted(positionId("Atendente"))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("CLINIC_REQUIRED"));
    }

    // ---------- migration ----------

    @Test
    @DisplayName("o seed traz 3 cargos, 9 permissões e a matriz combinada")
    void seededMatrix() {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM positions", Integer.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM permissions", Integer.class)).isEqualTo(9);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM position_permissions pp JOIN positions p ON p.id = pp.position_id
                WHERE p.name = 'Atendente'""", Integer.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM position_permissions pp JOIN positions p ON p.id = pp.position_id
                WHERE p.name = 'Gestor da clínica'""", Integer.class)).isEqualTo(6);
    }
}
