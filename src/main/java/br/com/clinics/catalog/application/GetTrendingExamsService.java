package br.com.clinics.catalog.application;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

import br.com.clinics.catalog.application.port.in.GetTrendingExamsUseCase;
import br.com.clinics.catalog.application.port.out.TrendingExamRepository;
import br.com.clinics.catalog.domain.TrendingExam;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class GetTrendingExamsService implements GetTrendingExamsUseCase {

    /** RN30: janela de 7 dias e até 5 itens. */
    static final Duration WINDOW = Duration.ofDays(7);
    static final int LIMIT = 5;

    private final TrendingExamRepository trending;
    private final Clock clock;

    GetTrendingExamsService(TrendingExamRepository trending, Clock clock) {
        this.trending = trending;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrendingExam> getTrending() {
        return trending.findTrending(clock.instant().minus(WINDOW), LIMIT);
    }
}
