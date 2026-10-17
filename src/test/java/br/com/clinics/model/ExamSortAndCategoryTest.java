package br.com.clinics.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import br.com.clinics.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExamSortAndCategoryTest {

    private static ExamSearchItem item(String name, String price, String rating, Double distance) {
        ExamOffer o = new ExamOffer(UUID.randomUUID(), UUID.randomUUID(), name, ExamCategory.SANGUE, UUID.randomUUID(),
                "Clínica", null, null, new BigDecimal(rating), true, "5581999999999", null, new BigDecimal(price),
                new BigDecimal(price), new BigDecimal(price), null, null);
        return new ExamSearchItem(o, distance);
    }

    private static List<String> names(ExamSort sort, ExamSearchItem... items) {
        return List.of(items).stream().sorted(sort.comparator()).map(i -> i.offer().examName() + "@" + i.offer().price()).toList();
    }

    @Test
    void sortsByPriceAscendingThenName() {
        assertThat(names(ExamSort.PRICE, item("B", "20", "4", null), item("A", "20", "4", null), item("C", "10", "4", null)))
                .containsExactly("C@10", "A@20", "B@20");
    }

    @Test
    void sortsByRatingDescending() {
        assertThat(names(ExamSort.RATING, item("A", "10", "4.5", null), item("B", "10", "4.9", null)))
                .containsExactly("B@10", "A@10");
    }

    @Test
    void sortsByNameIgnoringCase() {
        assertThat(names(ExamSort.NAME, item("b", "10", "4", null), item("A", "10", "4", null))).containsExactly("A@10", "b@10");
    }

    @Test
    void sortsByDistanceWithUnknownDistanceLast() {
        assertThat(names(ExamSort.DISTANCE, item("A", "10", "4", null), item("B", "20", "4", 2.0), item("C", "30", "4", 0.4)))
                .containsExactly("C@30", "B@20", "A@10");
    }

    @Test
    void sortDefaultsToPriceAndRejectsUnknownValues() {
        assertThat(ExamSort.parse(null)).isEqualTo(ExamSort.PRICE);
        assertThat(ExamSort.parse(" Distance ")).isEqualTo(ExamSort.DISTANCE);
        assertThatThrownBy(() -> ExamSort.parse("cheapest")).isInstanceOf(ValidationException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Raio-X", "raio_x", "RAIO X", "raiox"})
    void parsesCategoryByLabelOrName(String raw) {
        assertThat(ExamCategory.parse(raw)).isEqualTo(ExamCategory.RAIO_X);
    }

    @Test
    void rejectsUnknownCategory() {
        assertThatThrownBy(() -> ExamCategory.parse("Dentista")).isInstanceOf(ValidationException.class);
    }
}
