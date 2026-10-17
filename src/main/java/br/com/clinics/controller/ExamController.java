package br.com.clinics.controller;

import java.math.BigDecimal;
import java.util.UUID;

import br.com.clinics.service.GetExamDetailsService;
import br.com.clinics.service.SearchExamsService;
import br.com.clinics.dto.SearchExamsQuery;
import br.com.clinics.dto.ExamDtos.ExamDetailResponse;
import br.com.clinics.dto.ExamDtos.SearchResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/exams")
@Tag(name = "Exames")
public class ExamController {

    private final SearchExamsService search;
    private final GetExamDetailsService details;

    public ExamController(SearchExamsService search, GetExamDetailsService details) {
        this.search = search;
        this.details = details;
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
            @RequestParam(required = false) Integer size,
            @Parameter(description = "Bairro da clínica; ignora caixa e acentos") @RequestParam(required = false) String neighborhood,
            @Parameter(description = "Preço mínimo em reais") @RequestParam(name = "min_price", required = false) BigDecimal minPrice,
            @Parameter(description = "Preço máximo em reais") @RequestParam(name = "max_price", required = false) BigDecimal maxPrice,
            @Parameter(description = "Nota mínima da clínica, de 0 a 5") @RequestParam(name = "min_rating", required = false) BigDecimal minRating,
            @Parameter(description = "Somente as ofertas de menor preço") @RequestParam(name = "best_price_only", required = false) Boolean bestPriceOnly) {
        return SearchResponse.from(search.search(new SearchExamsQuery(query, category, sortBy, lat, lng, page, size,
                neighborhood, minPrice, maxPrice, minRating, bestPriceOnly)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhes de uma oferta de exame (UC04)")
    ExamDetailResponse details(@PathVariable UUID id, @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng) {
        return ExamDetailResponse.from(details.getById(id, lat, lng));
    }
}
