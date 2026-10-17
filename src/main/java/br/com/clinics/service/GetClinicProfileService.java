package br.com.clinics.service;

import br.com.clinics.dto.ClinicProfile;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.UUID;

import br.com.clinics.service.GetClinicProfileService;
import br.com.clinics.repository.ClinicRepository;
import br.com.clinics.repository.ExamOfferRepository;
import br.com.clinics.model.Clinic;
import br.com.clinics.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetClinicProfileService {

    private final ClinicRepository clinics;
    private final ExamOfferRepository offers;
    private final Clock clock;

    public GetClinicProfileService(ClinicRepository clinics, ExamOfferRepository offers, Clock clock) {
        this.clinics = clinics;
        this.offers = offers;
        this.clock = clock;
    }
    @Transactional(readOnly = true)
    public ClinicProfile getById(UUID clinicId) {
        Clinic clinic = clinics.findById(clinicId)
                .orElseThrow(() -> new NotFoundException("CLINIC_NOT_FOUND", "Clínica não encontrada."));
        return new ClinicProfile(clinic, clinic.isOpenAt(ZonedDateTime.now(clock)), offers.findAvailableByClinic(clinicId));
    }
}
