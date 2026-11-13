package br.com.clinics.common;

import java.util.List;
import java.util.function.Function;

/** Página de resultados. A página começa em 0, com 20 itens por padrão e no máximo 50. */
public record Page<T>(long total, int page, int size, int totalPages, List<T> data) {

    public static int page(Integer page) {
        return page == null ? 0 : Math.max(page, 0);
    }

    public static int size(Integer size) {
        return size == null ? 20 : Math.min(Math.max(size, 1), 50);
    }

    public static <T> Page<T> of(List<T> data, int page, int size, long total) {
        return new Page<>(total, page, size, (int) Math.ceil((double) total / size), data);
    }

    public <R> Page<R> map(Function<T, R> mapper) {
        return new Page<>(total, page, size, totalPages, data.stream().map(mapper).toList());
    }
}
