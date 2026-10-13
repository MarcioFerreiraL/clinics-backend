package br.com.clinics.catalog.application.port.in;

import java.util.List;

import br.com.clinics.catalog.domain.TrendingExam;

/** UC06 · Ver exames em alta. */
public interface GetTrendingExamsUseCase {

    List<TrendingExam> getTrending();
}
