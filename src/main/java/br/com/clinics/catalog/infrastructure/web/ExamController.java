package br.com.clinics.catalog.infrastructure.web;

import br.com.clinics.catalog.application.port.in.SearchExamsUseCase;
import br.com.clinics.catalog.application.port.in.SearchExamsUseCase.SearchExamsQuery;
import br.com.clinics.catalog.infrastructure.web.ExamDtos.SearchResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/exams")
@Tag(name = "Exames")
class ExamController {

    private final SearchExamsUseCase search;

    ExamController(SearchExamsUseCase search) {
        this.search = search;
    }

    @GetMapping("/search")
    @Operation(summary = "Busca e compara ofertas de exames (UC03)")
    SearchResponse search(
            @Parameter(description = "Texto do exame; ignora caixa e acentos") @RequestParam(required = false) String query,
            @Parameter(description = "Sangue, Raio-X, Consultas, Cardio ou Ultrassom") @RequestParam(required = false) String category,
            @Parameter(description = "price (padrão), rating, name ou distance") @RequestParam(name = "sort_by", required = false) String sortBy,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return SearchResponse.from(search.search(new SearchExamsQuery(query, category, sortBy, lat, lng, page, size)));
    }
}
