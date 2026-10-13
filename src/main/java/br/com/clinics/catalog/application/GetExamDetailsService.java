package br.com.clinics.catalog.application;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.UUID;

import br.com.clinics.catalog.application.port.in.GetExamDetailsUseCase;
import br.com.clinics.catalog.application.port.out.ClinicRepository;
import br.com.clinics.catalog.application.port.out.ExamOfferRepository;
import br.com.clinics.catalog.domain.Clinic;
import br.com.clinics.catalog.domain.ExamOffer;
import br.com.clinics.catalog.domain.GeoPoint;
import br.com.clinics.shared.domain.NotFoundException;
import br.com.clinics.shared.domain.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class GetExamDetailsService implements GetExamDetailsUseCase {

    private final ExamOfferRepository offers;
    private final ClinicRepository clinics;
    private final Clock clock;

    GetExamDetailsService(ExamOfferRepository offers, ClinicRepository clinics, Clock clock) {
        this.offers = offers;
        this.clinics = clinics;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public ExamDetails getById(UUID examId, Double latitude, Double longitude) {
        if ((latitude == null) != (longitude == null)) {
            throw new ValidationException("LOCATION_INCOMPLETE", "Informe lat e lng juntos.");
        }
        GeoPoint origin = latitude == null ? null : new GeoPoint(latitude, longitude);

        ExamOffer offer = offers.findAvailableById(examId)
                .orElseThrow(() -> new NotFoundException("EXAM_NOT_FOUND", "Exame não encontrado."));
        Clinic clinic = clinics.findById(offer.clinicId())
                .orElseThrow(() -> new NotFoundException("CLINIC_NOT_FOUND", "Clínica não encontrada."));

        return new ExamDetails(offer, clinic, clinic.isOpenAt(ZonedDateTime.now(clock)), offer.distanceKmFrom(origin));
    }
}
