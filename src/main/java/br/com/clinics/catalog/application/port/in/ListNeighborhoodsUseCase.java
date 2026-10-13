package br.com.clinics.catalog.application.port.in;

import java.util.List;

import br.com.clinics.catalog.domain.NeighborhoodSummary;

/** UC13 · Listar bairros atendidos. */
public interface ListNeighborhoodsUseCase {

    List<NeighborhoodSummary> list();
}
