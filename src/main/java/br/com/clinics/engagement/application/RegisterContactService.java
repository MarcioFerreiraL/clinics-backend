package br.com.clinics.engagement.application;

import java.net.URI;
import java.time.Clock;
import java.util.UUID;

import br.com.clinics.engagement.application.port.in.RegisterContactUseCase;
import br.com.clinics.engagement.application.port.out.ContactEventRepository;
import br.com.clinics.engagement.application.port.out.ContactableExamLookup;
import br.com.clinics.engagement.application.port.out.ContactableExamLookup.ContactableExam;
import br.com.clinics.engagement.domain.ContactEvent;
import br.com.clinics.engagement.domain.WhatsAppLinkBuilder;
import br.com.clinics.shared.domain.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class RegisterContactService implements RegisterContactUseCase {

    private final ContactableExamLookup exams;
    private final ContactEventRepository events;
    private final Clock clock;

    RegisterContactService(ContactableExamLookup exams, ContactEventRepository events, Clock clock) {
        this.exams = exams;
        this.events = events;
        this.clock = clock;
    }

    @Override
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
