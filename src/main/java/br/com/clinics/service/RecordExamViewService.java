package br.com.clinics.service;

import java.time.Clock;
import java.util.UUID;

import br.com.clinics.repository.ExamViewEventRepository;
import org.springframework.stereotype.Service;

@Service
public class RecordExamViewService {

    private final ExamViewEventRepository events;
    private final Clock clock;

    public RecordExamViewService(ExamViewEventRepository events, Clock clock) {
        this.events = events;
        this.clock = clock;
    }

    public void record(UUID examId) {
        events.record(examId, clock.instant());
    }
}
