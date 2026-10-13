package br.com.clinics.engagement.application;

import java.util.UUID;

import br.com.clinics.engagement.application.port.in.GetSavingsUseCase;
import br.com.clinics.engagement.application.port.out.ContactEventRepository;
import br.com.clinics.engagement.domain.SavingsSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class GetSavingsService implements GetSavingsUseCase {

    private final ContactEventRepository events;

    GetSavingsService(ContactEventRepository events) {
        this.events = events;
    }

    @Override
    @Transactional(readOnly = true)
    public SavingsSummary getSavings(UUID userId) {
        return events.summarizeByUser(userId);
    }
}
