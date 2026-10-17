package br.com.clinics.service;

import br.com.clinics.dto.ExamDetails;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.UUID;

import br.com.clinics.service.GetExamDetailsService;
import br.com.clinics.repository.ClinicRepository;
import br.com.clinics.repository.ExamOfferRepository;
import br.com.clinics.model.Clinic;
import br.com.clinics.model.ExamOffer;
import br.com.clinics.model.GeoPoint;
import br.com.clinics.exception.NotFoundException;
import br.com.clinics.exception.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetExamDetailsService {

    private final ExamOfferRepository offers;
    private final ClinicRepository clinics;
    private final Clock clock;

    public GetExamDetailsService(ExamOfferRepository offers, ClinicRepository clinics, Clock clock) {
        this.offers = offers;
        this.clinics = clinics;
        this.clock = clock;
    }
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
