package br.com.clinics.catalog.application;

import java.util.List;

import br.com.clinics.catalog.application.port.in.SearchExamsUseCase;
import br.com.clinics.catalog.application.port.out.ExamOfferRepository;
import br.com.clinics.catalog.domain.ExamCategory;
import br.com.clinics.catalog.domain.ExamOffer;
import br.com.clinics.catalog.domain.ExamSearchItem;
import br.com.clinics.catalog.domain.ExamSort;
import br.com.clinics.catalog.domain.GeoPoint;
import br.com.clinics.shared.domain.PageResult;
import br.com.clinics.shared.domain.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class SearchExamsService implements SearchExamsUseCase {

    private final ExamOfferRepository offers;

    SearchExamsService(ExamOfferRepository offers) {
        this.offers = offers;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ExamSearchItem> search(SearchExamsQuery query) {
        ExamCategory category = isBlank(query.category()) ? null : ExamCategory.parse(query.category());
        ExamSort sort = ExamSort.parse(query.sortBy());
        GeoPoint origin = originOf(query);
        if (sort == ExamSort.DISTANCE && origin == null) {
            throw new ValidationException("LOCATION_REQUIRED", "Informe lat e lng para ordenar por distância.");
        }
        int page = query.page() == null ? 0 : Math.max(query.page(), 0);
        int size = query.size() == null ? PageResult.DEFAULT_SIZE : Math.min(Math.max(query.size(), 1), PageResult.MAX_SIZE);

        // O volume previsto é pequeno (até alguns milhares de ofertas em uma cidade, RNF03), então ordenamos e
        // paginamos em memória para manter as regras de distância e ranking no domínio, testáveis sem banco.
        List<ExamSearchItem> sorted = offers.findAvailable(normalizeText(query.text()), category).stream()
                .map(offer -> new ExamSearchItem(offer, offer.distanceKmFrom(origin)))
                .sorted(sort.comparator())
                .toList();

        int from = (int) Math.min((long) page * size, sorted.size());
        int to = Math.min(from + size, sorted.size());
        return new PageResult<>(sorted.subList(from, to), page, size, sorted.size());
    }

    private static GeoPoint originOf(SearchExamsQuery query) {
        if (query.latitude() == null && query.longitude() == null) {
            return null;
        }
        if (query.latitude() == null || query.longitude() == null) {
            throw new ValidationException("LOCATION_INCOMPLETE", "Informe lat e lng juntos.");
        }
        return new GeoPoint(query.latitude(), query.longitude());
    }

    private static String normalizeText(String text) {
        return isBlank(text) ? null : text.strip();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
