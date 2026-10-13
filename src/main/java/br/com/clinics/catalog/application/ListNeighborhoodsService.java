package br.com.clinics.catalog.application;

import java.util.List;

import br.com.clinics.catalog.application.port.in.ListNeighborhoodsUseCase;
import br.com.clinics.catalog.application.port.out.ClinicRepository;
import br.com.clinics.catalog.domain.NeighborhoodSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ListNeighborhoodsService implements ListNeighborhoodsUseCase {

    private final ClinicRepository clinics;

    ListNeighborhoodsService(ClinicRepository clinics) {
        this.clinics = clinics;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NeighborhoodSummary> list() {
        return clinics.listNeighborhoods();
    }
}
