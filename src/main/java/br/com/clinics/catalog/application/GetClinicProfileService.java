package br.com.clinics.catalog.application;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.UUID;

import br.com.clinics.catalog.application.port.in.GetClinicProfileUseCase;
import br.com.clinics.catalog.application.port.out.ClinicRepository;
import br.com.clinics.catalog.application.port.out.ExamOfferRepository;
import br.com.clinics.catalog.domain.Clinic;
import br.com.clinics.shared.domain.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class GetClinicProfileService implements GetClinicProfileUseCase {

    private final ClinicRepository clinics;
    private final ExamOfferRepository offers;
    private final Clock clock;

    GetClinicProfileService(ClinicRepository clinics, ExamOfferRepository offers, Clock clock) {
        this.clinics = clinics;
        this.offers = offers;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public ClinicProfile getById(UUID clinicId) {
        Clinic clinic = clinics.findById(clinicId)
                .orElseThrow(() -> new NotFoundException("CLINIC_NOT_FOUND", "Clínica não encontrada."));
        return new ClinicProfile(clinic, clinic.isOpenAt(ZonedDateTime.now(clock)), offers.findAvailableByClinic(clinicId));
    }
}
