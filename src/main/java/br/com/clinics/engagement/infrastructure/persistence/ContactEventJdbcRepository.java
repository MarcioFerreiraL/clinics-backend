package br.com.clinics.engagement.infrastructure.persistence;

import java.sql.Timestamp;

import br.com.clinics.engagement.application.port.out.ContactEventRepository;
import br.com.clinics.engagement.domain.ContactEvent;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class ContactEventJdbcRepository implements ContactEventRepository {

    private final JdbcClient jdbc;

    ContactEventJdbcRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
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
}
