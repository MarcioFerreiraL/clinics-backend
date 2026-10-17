package br.com.clinics.dto;

import java.util.UUID;
import br.com.clinics.model.Clinic;
import br.com.clinics.model.ExamOffer;

public record ExamDetails(ExamOffer offer, Clinic clinic, boolean clinicOpenNow, Double distanceKm) {
}
