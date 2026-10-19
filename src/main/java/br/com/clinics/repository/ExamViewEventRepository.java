package br.com.clinics.repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ExamViewEventRepository {

    private final JdbcClient jdbc;

    public ExamViewEventRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void record(UUID examId, Instant createdAt) {
        jdbc.sql("INSERT INTO exam_view_events (exam_id, created_at) VALUES (:examId, :createdAt)")
                .param("examId", examId)
                .param("createdAt", Timestamp.from(createdAt))
                .update();
    }
}
