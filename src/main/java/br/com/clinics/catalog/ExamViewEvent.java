package br.com.clinics.catalog;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Uma abertura dos detalhes de um exame; alimenta o painel da clínica. */
@Entity
@Table(name = "exam_view_events")
public class ExamViewEvent {

    @Id
    private UUID id;
    private UUID examId;
    private Instant createdAt;

    protected ExamViewEvent() {
    }

    ExamViewEvent(UUID examId, Instant createdAt) {
        this.id = UUID.randomUUID();
        this.examId = examId;
        this.createdAt = createdAt;
    }
}
