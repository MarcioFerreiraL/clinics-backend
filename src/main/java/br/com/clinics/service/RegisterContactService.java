package br.com.clinics.service;

import br.com.clinics.dto.ContactResult;

import java.net.URI;
import java.time.Clock;
import java.util.UUID;

import br.com.clinics.service.RegisterContactService;
import br.com.clinics.repository.ContactEventRepository;
import br.com.clinics.service.ContactableExamLookup;
import br.com.clinics.dto.ContactableExam;
import br.com.clinics.model.ContactEvent;
import br.com.clinics.model.WhatsAppLinkBuilder;
import br.com.clinics.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterContactService {

    private final ContactableExamLookup exams;
    private final ContactEventRepository events;
    private final Clock clock;

    public RegisterContactService(ContactableExamLookup exams, ContactEventRepository events, Clock clock) {
        this.exams = exams;
        this.events = events;
        this.clock = clock;
    }
    @Transactional
    public ContactResult register(UUID examId, UUID userId) {
        ContactableExam exam = exams.findById(examId)
                .orElseThrow(() -> new NotFoundException("EXAM_NOT_FOUND", "Exame não encontrado."));

        URI url = WhatsAppLinkBuilder.build(exam.whatsappNumber(), exam.examName(), exam.price());
        ContactEvent event = ContactEvent.of(exam.examId(), userId, exam.price(), exam.savings(), clock.instant());
        events.save(event);

        return new ContactResult(event.id(), url, event.estimatedSavings());
    }
}
