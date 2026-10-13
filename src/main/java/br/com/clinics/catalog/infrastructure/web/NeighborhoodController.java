package br.com.clinics.catalog.infrastructure.web;

import java.util.List;

import br.com.clinics.catalog.application.port.in.ListNeighborhoodsUseCase;
import br.com.clinics.catalog.domain.NeighborhoodSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/neighborhoods")
@Tag(name = "Clínicas")
class NeighborhoodController {

    private final ListNeighborhoodsUseCase neighborhoods;

    NeighborhoodController(ListNeighborhoodsUseCase neighborhoods) {
        this.neighborhoods = neighborhoods;
    }

    @GetMapping
    @Operation(summary = "Bairros com clínicas cadastradas (UC13)")
    List<NeighborhoodResponse> list() {
        return neighborhoods.list().stream().map(NeighborhoodResponse::from).toList();
    }

    record NeighborhoodResponse(String name, int clinicsCount) {

        static NeighborhoodResponse from(NeighborhoodSummary n) {
            return new NeighborhoodResponse(n.name(), n.clinicsCount());
        }
    }
}
