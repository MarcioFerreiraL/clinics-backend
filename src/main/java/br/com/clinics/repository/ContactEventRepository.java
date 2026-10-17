package br.com.clinics.repository;

import java.sql.Timestamp;
import java.util.UUID;

import br.com.clinics.repository.ContactEventRepository;
import br.com.clinics.model.ContactEvent;
import br.com.clinics.model.SavingsSummary;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ContactEventRepository {

    private final JdbcClient jdbc;

    public ContactEventRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }
    public void save(ContactEvent e) {
        jdbc.sql("""
                        INSERT INTO contact_events (id, exam_id, user_id, price_at_contact, estimated_savings, created_at)
                        VALUES (:id, :examId, :userId, :price, :savings, :createdAt)""")
                .param("id", e.id())
                .param("examId", e.examId())
                .param("userId", e.userId(), java.sql.Types.OTHER)
                .param("price", e.priceAtContact())
                .param("savings", e.estimatedSavings())
                .param("createdAt", Timestamp.from(e.createdAt()))
                .update();
    }
    public SavingsSummary summarizeByUser(UUID userId) {
        return jdbc.sql("""
                        SELECT COALESCE(SUM(estimated_savings), 0) AS total, COUNT(*) AS contacts
                        FROM contact_events WHERE user_id = :userId""")
                .param("userId", userId)
                .query((rs, row) -> new SavingsSummary(rs.getBigDecimal("total").setScale(2), rs.getLong("contacts")))
                .single();
    }
}
