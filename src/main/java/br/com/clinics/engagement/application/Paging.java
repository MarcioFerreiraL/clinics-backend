package br.com.clinics.engagement.application;

import br.com.clinics.shared.domain.PageResult;

/** RN21: página começa em 0, tamanho padrão 20 e máximo 50. */
final class Paging {

    private Paging() {
    }

    static int page(Integer page) {
        return page == null ? 0 : Math.max(page, 0);
    }

    static int size(Integer size) {
        return size == null ? PageResult.DEFAULT_SIZE : Math.min(Math.max(size, 1), PageResult.MAX_SIZE);
    }
}
