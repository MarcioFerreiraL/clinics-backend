package br.com.clinics.catalog;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ContactEventRepository extends JpaRepository<ContactEvent, UUID> {

    @Query("""
            SELECT coalesce(sum(c.estimatedSavings), 0) AS total, count(c) AS contacts
            FROM ContactEvent c WHERE c.userId = :userId""")
    SavingsRow summarizeByUser(UUID userId);

    @Query("""
            SELECT count(c) FROM ContactEvent c, Exam e
            WHERE e.id = c.examId AND e.clinicId = :clinicId AND c.createdAt >= :since""")
    long countByClinicSince(UUID clinicId, Instant since);

    interface SavingsRow {

        BigDecimal getTotal();

        long getContacts();
    }
}
