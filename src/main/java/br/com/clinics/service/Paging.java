package br.com.clinics.service;

import br.com.clinics.dto.PageResult;

/** RN21: página começa em 0, tamanho padrão 20 e máximo 50. */
public final class Paging {

    private Paging() {
    }

    static int page(Integer page) {
        return page == null ? 0 : Math.max(page, 0);
    }

    static int size(Integer size) {
        return size == null ? PageResult.DEFAULT_SIZE : Math.min(Math.max(size, 1), PageResult.MAX_SIZE);
    }
}
