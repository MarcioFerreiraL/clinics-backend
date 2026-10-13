package br.com.clinics.engagement.application.port.in;

import java.util.UUID;

import br.com.clinics.engagement.domain.FavoriteExam;
import br.com.clinics.shared.domain.PageResult;

/** UC09 · Favoritar exames. Todas as operações valem para o usuário do token. */
public interface ManageFavoritesUseCase {

    /** Idempotente (RN23): favoritar de novo não altera nada. */
    void add(UUID userId, UUID examTypeId);

    /** Idempotente: remover quem não é favorito também é sucesso. */
    void remove(UUID userId, UUID examTypeId);

    PageResult<FavoriteExam> list(UUID userId, Integer page, Integer size);
}
