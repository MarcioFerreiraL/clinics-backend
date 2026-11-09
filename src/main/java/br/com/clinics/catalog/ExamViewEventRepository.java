package br.com.clinics.catalog;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ExamViewEventRepository extends JpaRepository<ExamViewEvent, UUID> {

    @Query("""
            SELECT count(v) FROM ExamViewEvent v, Exam e
            WHERE e.id = v.examId AND e.clinicId = :clinicId AND v.createdAt >= :since""")
    long countByClinicSince(UUID clinicId, Instant since);
}
