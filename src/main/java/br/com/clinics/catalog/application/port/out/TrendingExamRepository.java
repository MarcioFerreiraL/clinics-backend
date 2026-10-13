package br.com.clinics.catalog.application.port.out;

import java.time.Instant;
import java.util.List;

import br.com.clinics.catalog.domain.TrendingExam;

public interface TrendingExamRepository {

    /**
     * Tipos com ofertas disponíveis, do mais contatado desde {@code since} para o menos.
     * Empates e tipos sem contatos desempatam pelo menor preço médio (RN30).
     */
    List<TrendingExam> findTrending(Instant since, int limit);
}
