package br.com.clinics.service;

import br.com.clinics.dto.ContactableExam;

import java.util.Optional;
import java.util.UUID;

import br.com.clinics.service.GetExamDetailsService;
import br.com.clinics.service.ContactableExamLookup;
import br.com.clinics.exception.NotFoundException;
import org.springframework.stereotype.Component;

/** Busca os dados do exame (preço, economia e WhatsApp da clínica) usados para registrar um contato. */
@Component
public class ContactableExamLookup {

    private final GetExamDetailsService catalog;

    public ContactableExamLookup(GetExamDetailsService catalog) {
        this.catalog = catalog;
    }
    public Optional<ContactableExam> findById(UUID examId) {
        try {
            var offer = catalog.getById(examId, null, null).offer();
            return Optional.of(new ContactableExam(offer.examId(), offer.examName(), offer.price(), offer.savings(),
                    offer.whatsappNumber()));
        } catch (NotFoundException notFound) {
            return Optional.empty();
        }
    }
}
