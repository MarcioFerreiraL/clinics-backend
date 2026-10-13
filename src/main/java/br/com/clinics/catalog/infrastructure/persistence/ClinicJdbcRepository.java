package br.com.clinics.catalog.infrastructure.persistence;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.com.clinics.catalog.application.port.out.ClinicRepository;
import br.com.clinics.catalog.domain.Clinic;
import br.com.clinics.catalog.domain.GeoPoint;
import br.com.clinics.catalog.domain.OperatingHours;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class ClinicJdbcRepository implements ClinicRepository {

    private final JdbcClient jdbc;

    ClinicJdbcRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Clinic> findById(UUID id) {
        List<OperatingHours> hours = jdbc.sql("""
                        SELECT day_of_week, opens_at, closes_at FROM clinic_operating_hours
                        WHERE clinic_id = :id ORDER BY day_of_week, opens_at""")
                .param("id", id)
                .query((rs, row) -> new OperatingHours(DayOfWeek.of(rs.getInt("day_of_week")),
                        rs.getTime("opens_at").toLocalTime(), rs.getTime("closes_at").toLocalTime()))
                .list();

        return jdbc.sql("SELECT * FROM clinics WHERE id = :id")
                .param("id", id)
                .query((rs, row) -> map(rs, hours))
                .optional();
    }

    private static Clinic map(ResultSet rs, List<OperatingHours> hours) throws SQLException {
        BigDecimal lat = rs.getBigDecimal("latitude");
        BigDecimal lng = rs.getBigDecimal("longitude");
        GeoPoint location = lat == null || lng == null ? null : new GeoPoint(lat.doubleValue(), lng.doubleValue());
        return new Clinic(rs.getObject("id", UUID.class), rs.getString("name"), rs.getString("description"),
                rs.getString("address"), rs.getString("neighborhood"), rs.getString("city"), rs.getString("state"),
                location, rs.getString("phone_number"), rs.getString("whatsapp_number"), rs.getString("logo_url"),
                rs.getBigDecimal("rating"), rs.getInt("review_count"), rs.getInt("patient_count"),
                rs.getBoolean("is_verified"), hours);
    }
}
