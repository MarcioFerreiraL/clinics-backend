package br.com.clinics.service;

import java.util.List;

import br.com.clinics.service.ListNeighborhoodsService;
import br.com.clinics.repository.ClinicRepository;
import br.com.clinics.model.NeighborhoodSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListNeighborhoodsService {

    private final ClinicRepository clinics;

    public ListNeighborhoodsService(ClinicRepository clinics) {
        this.clinics = clinics;
    }
    @Transactional(readOnly = true)
    public List<NeighborhoodSummary> list() {
        return clinics.listNeighborhoods();
    }
}
