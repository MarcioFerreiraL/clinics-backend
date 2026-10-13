package br.com.clinics.engagement.application.port.out;

import java.util.List;
import java.util.UUID;

import br.com.clinics.engagement.domain.FavoriteExam;

public interface FavoriteRepository {

    boolean examTypeExists(UUID examTypeId);

    void add(UUID userId, UUID examTypeId);

    void remove(UUID userId, UUID examTypeId);

    List<FavoriteExam> findByUser(UUID userId, int limit, int offset);

    long countByUser(UUID userId);
}
