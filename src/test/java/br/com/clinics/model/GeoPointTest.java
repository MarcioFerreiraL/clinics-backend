package br.com.clinics.model;

import br.com.clinics.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class GeoPointTest {

    @Test
    void distanceToItselfIsZero() {
        GeoPoint p = new GeoPoint(-7.8333, -35.7528);
        assertThat(p.distanceKmTo(p)).isZero();
    }

    @Test
    void computesKnownLongDistance() {
        GeoPoint saoPaulo = new GeoPoint(-23.5505, -46.6333);
        GeoPoint rio = new GeoPoint(-22.9068, -43.1729);
        assertThat(saoPaulo.distanceKmTo(rio)).isCloseTo(360.0, within(3.0));
    }

    @Test
    void computesShortDistanceInsideTheCity() {
        GeoPoint santaMaria = new GeoPoint(-7.8333, -35.7528);
        GeoPoint saoJose = new GeoPoint(-7.8412, -35.7601);
        assertThat(santaMaria.distanceKmTo(saoJose)).isCloseTo(1.2, within(0.1));
    }

    @Test
    void isSymmetric() {
        GeoPoint a = new GeoPoint(-7.83, -35.75);
        GeoPoint b = new GeoPoint(-7.84, -35.76);
        assertThat(a.distanceKmTo(b)).isEqualTo(b.distanceKmTo(a));
    }

    @Test
    void rejectsOutOfRangeCoordinates() {
        assertThatThrownBy(() -> new GeoPoint(91, 0)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> new GeoPoint(0, -181)).isInstanceOf(ValidationException.class);
    }
}
