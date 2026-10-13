package br.com.clinics.catalog.infrastructure.web;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import br.com.clinics.catalog.application.port.in.GetTrendingExamsUseCase;
import br.com.clinics.catalog.domain.TrendingExam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/exam-types")
@Tag(name = "Exames")
class ExamTypeController {

    private final GetTrendingExamsUseCase trending;

    ExamTypeController(GetTrendingExamsUseCase trending) {
        this.trending = trending;
    }

    @GetMapping("/trending")
    @Operation(summary = "Exames mais contatados nos últimos 7 dias (UC06)")
    List<TrendingResponse> trending() {
        return trending.getTrending().stream().map(TrendingResponse::from).toList();
    }

    record TrendingResponse(UUID id, String name, String slug, String category, BigDecimal minPrice, int offersCount,
            long recentContacts) {

        static TrendingResponse from(TrendingExam t) {
            return new TrendingResponse(t.examTypeId(), t.name(), t.slug(), t.category().label(), t.minPrice(),
                    t.offersCount(), t.recentContacts());
        }
    }
}
