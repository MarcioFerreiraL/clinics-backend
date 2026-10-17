package br.com.clinics.service;

import java.util.UUID;

import br.com.clinics.service.ManageFavoritesService;
import br.com.clinics.repository.FavoriteRepository;
import br.com.clinics.model.FavoriteExam;
import br.com.clinics.exception.NotFoundException;
import br.com.clinics.dto.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ManageFavoritesService {

    private final FavoriteRepository favorites;

    public ManageFavoritesService(FavoriteRepository favorites) {
        this.favorites = favorites;
    }
    @Transactional
    public void add(UUID userId, UUID examTypeId) {
        if (!favorites.examTypeExists(examTypeId)) {
            throw new NotFoundException("EXAM_TYPE_NOT_FOUND", "Exame não encontrado.");
        }
        favorites.add(userId, examTypeId);
    }
    @Transactional
    public void remove(UUID userId, UUID examTypeId) {
        favorites.remove(userId, examTypeId);
    }
    @Transactional(readOnly = true)
    public PageResult<FavoriteExam> list(UUID userId, Integer page, Integer size) {
        int p = Paging.page(page);
        int s = Paging.size(size);
        return new PageResult<>(favorites.findByUser(userId, s, p * s), p, s, favorites.countByUser(userId));
    }
}
