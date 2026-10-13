package br.com.clinics.catalog.application.port.out;

import java.util.Optional;
import java.util.UUID;

import br.com.clinics.catalog.domain.Clinic;

public interface ClinicRepository {

    Optional<Clinic> findById(UUID id);
}
