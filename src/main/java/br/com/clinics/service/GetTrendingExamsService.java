package br.com.clinics.service;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

import br.com.clinics.service.GetTrendingExamsService;
import br.com.clinics.repository.TrendingExamRepository;
import br.com.clinics.model.TrendingExam;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetTrendingExamsService {

    /** RN30: janela de 7 dias e até 5 itens. */
    static final Duration WINDOW = Duration.ofDays(7);
    static final int LIMIT = 5;

    private final TrendingExamRepository trending;
    private final Clock clock;

    public GetTrendingExamsService(TrendingExamRepository trending, Clock clock) {
        this.trending = trending;
        this.clock = clock;
    }
    @Transactional(readOnly = true)
    public List<TrendingExam> getTrending() {
        return trending.findTrending(clock.instant().minus(WINDOW), LIMIT);
    }
}
