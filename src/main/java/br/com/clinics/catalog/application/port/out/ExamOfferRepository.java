package br.com.clinics.catalog.application.port.out;

import java.util.List;

import br.com.clinics.catalog.domain.ExamCategory;
import br.com.clinics.catalog.domain.ExamOffer;

public interface ExamOfferRepository {

    /**
     * Ofertas disponíveis cujo tipo de exame casa com o texto (ignorando caixa e acentos, RN16) e a categoria.
     * Os filtros incidem sobre o tipo, então média e mínimo refletem todas as clínicas (RN14).
     */
    List<ExamOffer> findAvailable(String text, ExamCategory category);
}
