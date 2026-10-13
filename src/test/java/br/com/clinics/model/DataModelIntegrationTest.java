package br.com.clinics.model;

import java.util.List;
import java.util.UUID;

import br.com.clinics.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifica o modelo de dados (docs/05-modelo-de-dados.md) direto no PostgreSQL: tabelas, tipos,
 * restrições e chaves estrangeiras. Cada teste roda numa transação que é desfeita no fim.
 */
@IntegrationTest
@Transactional
class DataModelIntegrationTest {

    @Autowired JdbcTemplate jdbc;

    private UUID user(String phone, String email, String cpf) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO users (id, phone, email, cpf, password_hash, full_name) VALUES (?, ?, ?, ?, 'x', 'Teste')",
                id, phone, email, cpf);
        return id;
    }

    private UUID clinic(String name) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO clinics (id, name, address, whatsapp_number) VALUES (?, ?, 'Rua A, 1', '5581999990000')", id, name);
        return id;
    }

    private UUID examType(String name, String slug, String category) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO exam_types (id, name, slug, category) VALUES (?, ?, ?, ?)", id, name, slug, category);
        return id;
    }

    private UUID exam(UUID clinic, UUID type, String price) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO exams (id, clinic_id, exam_type_id, price) VALUES (?, ?, ?, ?::numeric)", id, clinic, type, price);
        return id;
    }

    private void assertRejected(Runnable statement) {
        assertThatThrownBy(statement::run).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("MER · todas as tabelas do modelo existem")
    void allTablesExist() {
        List<String> tables = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'", String.class);
        assertThat(tables).contains("users", "clinics", "clinic_operating_hours", "exam_types", "exams", "favorites",
                "reviews", "price_history", "contact_events", "exam_type_aliases", "flyway_schema_history");
    }

    @Test
    @DisplayName("RNF05 · todas as migrações Flyway foram aplicadas, em ordem e sem falha")
    void flywayMigrationsAreContiguousAndSuccessful() {
        List<Integer> versions = jdbc.queryForList(
                "SELECT version::int FROM flyway_schema_history WHERE success ORDER BY installed_rank", Integer.class);
        assertThat(versions).startsWith(1, 2, 3, 4, 5, 6, 7, 8, 9);
        Integer failed = jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE NOT success", Integer.class);
        assertThat(failed).isZero();
    }

    @Test
    @DisplayName("MER · tipos das colunas principais")
    void columnTypesMatchTheModel() {
        assertThat(columnType("users", "phone")).isEqualTo("character varying");
        assertThat(columnType("users", "birth_date")).isEqualTo("date");
        assertThat(columnType("exams", "price")).isEqualTo("numeric");
        assertThat(columnType("clinics", "latitude")).isEqualTo("numeric");
        assertThat(columnType("clinics", "rating")).isEqualTo("numeric");
        assertThat(columnType("users", "id")).isEqualTo("uuid");
        assertThat(columnType("contact_events", "created_at")).isEqualTo("timestamp with time zone");
    }

    @Test
    @DisplayName("RN11 · valores monetários são numeric com 2 casas (nunca ponto flutuante)")
    void moneyColumnsAreNumericWithTwoDecimals() {
        for (String[] col : new String[][] {{"exams", "price"}, {"price_history", "price"},
                {"contact_events", "price_at_contact"}, {"contact_events", "estimated_savings"}}) {
            Integer scale = jdbc.queryForObject(
                    "SELECT numeric_scale FROM information_schema.columns WHERE table_name = ? AND column_name = ?",
                    Integer.class, col[0], col[1]);
            assertThat(scale).as("%s.%s", col[0], col[1]).isEqualTo(2);
        }
    }

    @Test
    @DisplayName("RN20 / RNF09 · nenhuma tabela guarda a localização do paciente")
    void patientLocationIsNeverStored() {
        List<String> columns = jdbc.queryForList("""
                SELECT column_name FROM information_schema.columns
                WHERE table_name IN ('users', 'contact_events', 'favorites', 'reviews')
                  AND (column_name ILIKE '%lat%' OR column_name ILIKE '%lng%' OR column_name ILIKE '%lon%')""", String.class);
        assertThat(columns).isEmpty();
    }

    // ---------- users ----------

    @Test
    @DisplayName("RN01 · telefone é único")
    void phoneIsUnique() {
        user("5581999990001", null, null);
        assertRejected(() -> user("5581999990001", null, null));
    }

    @Test
    @DisplayName("RN04 · e-mail é único sem diferenciar maiúsculas, mas é opcional")
    void emailIsUniqueCaseInsensitiveAndOptional() {
        user("5581999990002", "a@teste.com", null);
        user("5581999990003", null, null);
        user("5581999990004", null, null);
        assertRejected(() -> user("5581999990005", "A@TESTE.COM", null));
    }

    @Test
    @DisplayName("RN37 · CPF é único, mas pode ser nulo nos dados de demonstração")
    void cpfIsUniqueWhenPresent() {
        user("5581999990006", null, null);
        user("5581999990007", null, null);
        user("5581999990008", null, "52998224725");
        assertRejected(() -> user("5581999990009", null, "52998224725"));
    }

    @Test
    @DisplayName("RN05 · papel só aceita PATIENT, CLINIC_MANAGER ou ADMIN")
    void roleIsRestricted() {
        assertRejected(() -> jdbc.update(
                "INSERT INTO users (phone, password_hash, full_name, role) VALUES ('5581999990010', 'x', 'T', 'SUPERUSER')"));
    }

    @Test
    @DisplayName("RN05 · papel padrão é PATIENT")
    void defaultRoleIsPatient() {
        UUID id = user("5581999990011", null, null);
        assertThat(jdbc.queryForObject("SELECT role FROM users WHERE id = ?", String.class, id)).isEqualTo("PATIENT");
    }

    // ---------- clinics ----------

    @Test
    @DisplayName("MER · nota da clínica fica entre 0 e 5")
    void clinicRatingBounds() {
        UUID id = clinic("Clínica Nota");
        assertRejected(() -> jdbc.update("UPDATE clinics SET rating = 5.1 WHERE id = ?", id));
    }

    @Test
    @DisplayName("MER · cidade e UF padrão são Surubim e PE")
    void clinicDefaults() {
        UUID id = clinic("Clínica Padrão");
        assertThat(jdbc.queryForMap("SELECT city, state, rating, review_count, is_verified FROM clinics WHERE id = ?", id))
                .containsEntry("city", "Surubim").containsEntry("state", "PE").containsEntry("review_count", 0)
                .containsEntry("is_verified", false);
    }

    @Test
    @DisplayName("MER · excluir o gestor não apaga a clínica (ON DELETE SET NULL)")
    void deletingOwnerKeepsTheClinic() {
        UUID owner = user("5581999990012", null, null);
        UUID clinic = clinic("Clínica do Gestor");
        jdbc.update("UPDATE clinics SET owner_user_id = ? WHERE id = ?", owner, clinic);

        jdbc.update("DELETE FROM users WHERE id = ?", owner);

        assertThat(jdbc.queryForObject("SELECT owner_user_id FROM clinics WHERE id = ?", UUID.class, clinic)).isNull();
    }

    // ---------- horários ----------

    @ParameterizedTest
    @ValueSource(ints = {0, 8})
    @DisplayName("RN22 · dia da semana vai de 1 a 7")
    void operatingDayBounds(int day) {
        UUID c = clinic("Clínica Horário");
        assertRejected(() -> jdbc.update(
                "INSERT INTO clinic_operating_hours (clinic_id, day_of_week, opens_at, closes_at) VALUES (?, ?, '08:00', '12:00')",
                c, day));
    }

    @Test
    @DisplayName("RN22 · o horário de fechamento é depois do de abertura")
    void closingAfterOpening() {
        UUID c = clinic("Clínica Fecha");
        assertRejected(() -> jdbc.update(
                "INSERT INTO clinic_operating_hours (clinic_id, day_of_week, opens_at, closes_at) VALUES (?, 1, '12:00', '08:00')", c));
    }

    @Test
    @DisplayName("RN22 · não repete o mesmo início no mesmo dia")
    void sameOpeningTimeIsUniquePerDay() {
        UUID c = clinic("Clínica Dobrada");
        jdbc.update("INSERT INTO clinic_operating_hours (clinic_id, day_of_week, opens_at, closes_at) VALUES (?, 1, '08:00', '12:00')", c);
        assertRejected(() -> jdbc.update(
                "INSERT INTO clinic_operating_hours (clinic_id, day_of_week, opens_at, closes_at) VALUES (?, 1, '08:00', '17:00')", c));
    }

    // ---------- catálogo ----------

    @Test
    @DisplayName("RN10 · categoria do exame só aceita as cinco definidas")
    void examCategoryIsRestricted() {
        assertRejected(() -> examType("Exame X", "exame-x", "ODONTOLOGIA"));
    }

    @Test
    @DisplayName("RN09 · nome e slug do tipo de exame são únicos")
    void examTypeNameAndSlugAreUnique() {
        examType("Exame Único", "exame-unico", "SANGUE");
        assertRejected(() -> examType("Exame Único", "outro-slug", "SANGUE"));
    }

    @Test
    @DisplayName("RN09 · cada clínica tem no máximo uma oferta por tipo de exame")
    void oneOfferPerClinicAndType() {
        UUID c = clinic("Clínica Oferta");
        UUID t = examType("Exame Oferta", "exame-oferta", "CARDIO");
        exam(c, t, "50.00");
        assertRejected(() -> exam(c, t, "60.00"));
    }

    @Test
    @DisplayName("RN11 · preço não pode ser negativo")
    void priceCannotBeNegative() {
        UUID c = clinic("Clínica Preço");
        UUID t = examType("Exame Preço", "exame-preco", "SANGUE");
        assertRejected(() -> exam(c, t, "-1.00"));
    }

    @Test
    @DisplayName("RN12 · oferta nasce disponível por padrão")
    void offerIsAvailableByDefault() {
        UUID id = exam(clinic("Clínica Disp"), examType("Exame Disp", "exame-disp", "SANGUE"), "10.00");
        assertThat(jdbc.queryForObject("SELECT available FROM exams WHERE id = ?", Boolean.class, id)).isTrue();
    }

    @Test
    @DisplayName("MER · excluir a clínica apaga suas ofertas e horários (CASCADE)")
    void deletingClinicCascades() {
        UUID c = clinic("Clínica Cascata");
        UUID e = exam(c, examType("Exame Cascata", "exame-cascata", "SANGUE"), "10.00");
        jdbc.update("INSERT INTO clinic_operating_hours (clinic_id, day_of_week, opens_at, closes_at) VALUES (?, 1, '08:00', '12:00')", c);

        jdbc.update("DELETE FROM clinics WHERE id = ?", c);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM exams WHERE id = ?", Integer.class, e)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM clinic_operating_hours WHERE clinic_id = ?", Integer.class, c)).isZero();
    }

    @Test
    @DisplayName("MER · o tipo de exame não pode ser apagado enquanto houver oferta")
    void examTypeInUseCannotBeDeleted() {
        UUID t = examType("Exame Em Uso", "exame-em-uso", "SANGUE");
        exam(clinic("Clínica Uso"), t, "10.00");
        assertRejected(() -> jdbc.update("DELETE FROM exam_types WHERE id = ?", t));
    }

    // ---------- engajamento ----------

    @Test
    @DisplayName("RN23 · favorito é único por paciente e tipo de exame (chave composta)")
    void favoriteIsUniquePerUserAndType() {
        UUID u = user("5581999990020", null, null);
        UUID t = examType("Exame Fav", "exame-fav", "SANGUE");
        jdbc.update("INSERT INTO favorites (user_id, exam_type_id) VALUES (?, ?)", u, t);
        assertRejected(() -> jdbc.update("INSERT INTO favorites (user_id, exam_type_id) VALUES (?, ?)", u, t));
    }

    @Test
    @DisplayName("RN25 · cada paciente tem uma avaliação por clínica")
    void oneReviewPerUserAndClinic() {
        UUID u = user("5581999990021", null, null);
        UUID c = clinic("Clínica Aval");
        jdbc.update("INSERT INTO reviews (clinic_id, user_id, rating) VALUES (?, ?, 5)", c, u);
        assertRejected(() -> jdbc.update("INSERT INTO reviews (clinic_id, user_id, rating) VALUES (?, ?, 4)", c, u));
    }

    @Test
    @DisplayName("RN26 · nota da avaliação é de 1 a 5")
    void reviewRatingBounds() {
        UUID u = user("5581999990022", null, null);
        UUID c = clinic("Clínica Nota 0");
        assertRejected(() -> jdbc.update("INSERT INTO reviews (clinic_id, user_id, rating) VALUES (?, ?, 0)", c, u));
    }

    @Test
    @DisplayName("RN26 · comentário tem até 1000 caracteres")
    void reviewCommentLimit() {
        UUID u = user("5581999990023", null, null);
        UUID c = clinic("Clínica Comentário");
        assertRejected(() -> jdbc.update("INSERT INTO reviews (clinic_id, user_id, rating, comment) VALUES (?, ?, 5, ?)",
                c, u, "a".repeat(1001)));
    }

    @Test
    @DisplayName("RN29 · contato aceita usuário nulo (visitante) e mantém o registro se o usuário for apagado")
    void contactKeepsHistoryWithoutUser() {
        UUID u = user("5581999990024", null, null);
        UUID e = exam(clinic("Clínica Contato"), examType("Exame Contato", "exame-contato", "SANGUE"), "10.00");
        jdbc.update("INSERT INTO contact_events (exam_id, user_id, price_at_contact, estimated_savings) VALUES (?, NULL, 10, 0)", e);
        jdbc.update("INSERT INTO contact_events (exam_id, user_id, price_at_contact, estimated_savings) VALUES (?, ?, 10, 2)", e, u);

        jdbc.update("DELETE FROM users WHERE id = ?", u);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM contact_events WHERE exam_id = ?", Integer.class, e)).isEqualTo(2);
    }

    @Test
    @DisplayName("RN31 · economia estimada não pode ser negativa")
    void estimatedSavingsCannotBeNegative() {
        UUID e = exam(clinic("Clínica Econ"), examType("Exame Econ", "exame-econ", "SANGUE"), "10.00");
        assertRejected(() -> jdbc.update(
                "INSERT INTO contact_events (exam_id, price_at_contact, estimated_savings) VALUES (?, 10, -1)", e));
    }

    @Test
    @DisplayName("RN13 · histórico de preço não aceita valor negativo")
    void priceHistoryCannotBeNegative() {
        UUID e = exam(clinic("Clínica Hist"), examType("Exame Hist", "exame-hist", "SANGUE"), "10.00");
        assertRejected(() -> jdbc.update("INSERT INTO price_history (exam_id, price) VALUES (?, -5)", e));
    }

    @Test
    @DisplayName("RN39 · apelido não se repete dentro do mesmo tipo de exame")
    void aliasIsUniquePerExamType() {
        UUID t = examType("Exame Apelido", "exame-apelido", "SANGUE");
        jdbc.update("INSERT INTO exam_type_aliases (exam_type_id, alias) VALUES (?, 'sangue geral')", t);
        assertRejected(() -> jdbc.update("INSERT INTO exam_type_aliases (exam_type_id, alias) VALUES (?, 'sangue geral')", t));
    }

    @Test
    @DisplayName("MER · índices de desempenho documentados existem")
    void documentedIndexesExist() {
        List<String> indexes = jdbc.queryForList("SELECT indexname FROM pg_indexes WHERE schemaname = 'public'", String.class);
        assertThat(indexes).contains("idx_exams_type_price", "idx_exams_clinic", "idx_reviews_clinic",
                "idx_price_history_exam", "idx_contact_events_created", "idx_contact_events_exam_created",
                "idx_contact_events_user", "uq_users_email", "uq_users_cpf");
    }

    private String columnType(String table, String column) {
        return jdbc.queryForObject(
                "SELECT data_type FROM information_schema.columns WHERE table_name = ? AND column_name = ?",
                String.class, table, column);
    }
}
