package br.com.clinics.catalog.application.port.in;

import br.com.clinics.catalog.domain.ExamSearchItem;
import br.com.clinics.shared.domain.PageResult;

/** UC03 · Buscar e comparar exames. */
public interface SearchExamsUseCase {

    PageResult<ExamSearchItem> search(SearchExamsQuery query);

    /** Valores brutos vindos da borda; a interpretação e a validação acontecem no caso de uso. */
    record SearchExamsQuery(String text, String category, String sortBy, Double latitude, Double longitude,
            Integer page, Integer size) {
    }
}
