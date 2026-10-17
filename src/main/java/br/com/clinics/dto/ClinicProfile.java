package br.com.clinics.dto;

import java.util.List;
import java.util.UUID;
import br.com.clinics.model.Clinic;
import br.com.clinics.model.ExamOffer;

public record ClinicProfile(Clinic clinic, boolean openNow, List<ExamOffer> offers) {
}
