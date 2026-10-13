package br.com.clinics.engagement.application;

import java.util.UUID;

import br.com.clinics.engagement.application.port.in.ManageFavoritesUseCase;
import br.com.clinics.engagement.application.port.out.FavoriteRepository;
import br.com.clinics.engagement.domain.FavoriteExam;
import br.com.clinics.shared.domain.NotFoundException;
import br.com.clinics.shared.domain.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ManageFavoritesService implements ManageFavoritesUseCase {

    private final FavoriteRepository favorites;

    ManageFavoritesService(FavoriteRepository favorites) {
        this.favorites = favorites;
    }

    @Override
    @Transactional
    public void add(UUID userId, UUID examTypeId) {
        if (!favorites.examTypeExists(examTypeId)) {
            throw new NotFoundException("EXAM_TYPE_NOT_FOUND", "Exame não encontrado.");
        }
        favorites.add(userId, examTypeId);
    }

    @Override
    @Transactional
    public void remove(UUID userId, UUID examTypeId) {
        favorites.remove(userId, examTypeId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<FavoriteExam> list(UUID userId, Integer page, Integer size) {
        int p = Paging.page(page);
        int s = Paging.size(size);
        return new PageResult<>(favorites.findByUser(userId, s, p * s), p, s, favorites.countByUser(userId));
    }
}
