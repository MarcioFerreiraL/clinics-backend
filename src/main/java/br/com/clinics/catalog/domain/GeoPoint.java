package br.com.clinics.catalog.domain;

import br.com.clinics.shared.domain.ValidationException;

/** Coordenada geográfica. A distância é a de círculo máximo (Haversine), RN19. */
public record GeoPoint(double latitude, double longitude) {

    private static final double EARTH_RADIUS_KM = 6371.0088;

    public GeoPoint {
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new ValidationException("INVALID_COORDINATES", "Latitude ou longitude fora do intervalo válido.");
        }
    }

    public double distanceKmTo(GeoPoint other) {
        double dLat = Math.toRadians(other.latitude - latitude);
        double dLng = Math.toRadians(other.longitude - longitude);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(latitude)) * Math.cos(Math.toRadians(other.latitude))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return EARTH_RADIUS_KM * 2 * Math.asin(Math.sqrt(a));
    }
}
