package br.com.clinics.dto;

import java.util.List;

/** Página de resultados independente de framework. */
public record PageResult<T>(List<T> items, int page, int size, long total) {

    public static final int MAX_SIZE = 50;
    public static final int DEFAULT_SIZE = 20;

    public int totalPages() {
        return size == 0 ? 0 : (int) Math.ceil((double) total / size);
    }
}
