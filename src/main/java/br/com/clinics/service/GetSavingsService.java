package br.com.clinics.service;

import java.util.UUID;

import br.com.clinics.service.GetSavingsService;
import br.com.clinics.repository.ContactEventRepository;
import br.com.clinics.model.SavingsSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetSavingsService {

    private final ContactEventRepository events;

    public GetSavingsService(ContactEventRepository events) {
        this.events = events;
    }
    @Transactional(readOnly = true)
    public SavingsSummary getSavings(UUID userId) {
        return events.summarizeByUser(userId);
    }
}
