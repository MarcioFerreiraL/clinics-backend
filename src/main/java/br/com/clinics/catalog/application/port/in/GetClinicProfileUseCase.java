package br.com.clinics.catalog.application.port.in;

import java.util.List;
import java.util.UUID;

import br.com.clinics.catalog.domain.Clinic;
import br.com.clinics.catalog.domain.ExamOffer;

/** UC05 · Ver perfil da clínica. */
public interface GetClinicProfileUseCase {

    ClinicProfile getById(UUID clinicId);

    record ClinicProfile(Clinic clinic, boolean openNow, List<ExamOffer> offers) {
    }
}
