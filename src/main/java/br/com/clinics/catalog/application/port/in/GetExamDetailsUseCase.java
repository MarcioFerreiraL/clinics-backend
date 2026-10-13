package br.com.clinics.catalog.application.port.in;

import java.util.UUID;

import br.com.clinics.catalog.domain.Clinic;
import br.com.clinics.catalog.domain.ExamOffer;

/** UC04 · Ver detalhes do exame. */
public interface GetExamDetailsUseCase {

    ExamDetails getById(UUID examId, Double latitude, Double longitude);

    record ExamDetails(ExamOffer offer, Clinic clinic, boolean clinicOpenNow, Double distanceKm) {
    }
}
