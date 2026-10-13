package br.com.clinics.engagement.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import br.com.clinics.catalog.application.port.in.GetExamDetailsUseCase;
import br.com.clinics.engagement.application.port.out.ContactableExamLookup;
import br.com.clinics.shared.domain.NotFoundException;
import org.springframework.stereotype.Component;

/** Adaptador que consome a porta pública do catálogo e traduz para o vocabulário do engagement. */
@Component
class CatalogContactableExamLookup implements ContactableExamLookup {

    private final GetExamDetailsUseCase catalog;

    CatalogContactableExamLookup(GetExamDetailsUseCase catalog) {
        this.catalog = catalog;
    }

    @Override
    public Optional<ContactableExam> findById(UUID examId) {
        try {
            var offer = catalog.getById(examId, null, null).offer();
            return Optional.of(new ContactableExam(offer.examId(), offer.examName(), offer.price(), offer.savings(),
                    offer.whatsappNumber()));
        } catch (NotFoundException notFound) {
            return Optional.empty();
        }
    }
}
