package br.com.clinics.controller;

import java.util.List;

import br.com.clinics.service.ListNeighborhoodsService;
import br.com.clinics.model.NeighborhoodSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/neighborhoods")
@Tag(name = "Clínicas")
public class NeighborhoodController {

    private final ListNeighborhoodsService neighborhoods;

    public NeighborhoodController(ListNeighborhoodsService neighborhoods) {
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
