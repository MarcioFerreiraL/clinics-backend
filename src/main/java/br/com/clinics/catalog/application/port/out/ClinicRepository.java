package br.com.clinics.catalog.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.com.clinics.catalog.domain.Clinic;
import br.com.clinics.catalog.domain.NeighborhoodSummary;

public interface ClinicRepository {

    Optional<Clinic> findById(UUID id);

    /** Bairros distintos com a quantidade de clínicas, em ordem alfabética. */
    List<NeighborhoodSummary> listNeighborhoods();
}
