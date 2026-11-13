package br.com.clinics.catalog;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Os horários vêm junto: toda tela que mostra a clínica mostra também se ela está aberta. */
@Entity
@Table(name = "clinics")
public class Clinic {

    @Id
    private UUID id;
    private UUID ownerUserId;
    private String name;
    private String description;
    private String address;
    private String neighborhood;
    private String city;
    @JdbcTypeCode(SqlTypes.CHAR)
    private String state;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String phoneNumber;
    private String whatsappNumber;
    private String logoUrl;
    private BigDecimal rating;
    private int reviewCount;
    private int patientCount;
    @Column(name = "is_verified")
    private boolean verified;
    private Instant updatedAt;
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "clinic_id", insertable = false, updatable = false)
    @OrderBy("dayOfWeek, opensAt")
    private List<OperatingHours> hours;

    protected Clinic() {
    }

    /** Aberta se o momento cai em algum intervalo do dia. */
    boolean isOpenAt(ZonedDateTime moment) {
        return hours.stream().anyMatch(h -> h.covers(moment));
    }

    public void updateRating(BigDecimal rating, int reviewCount, Instant now) {
        this.rating = rating;
        this.reviewCount = reviewCount;
        this.updatedAt = now;
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    String description() {
        return description;
    }

    String address() {
        return address;
    }

    String neighborhood() {
        return neighborhood;
    }

    String city() {
        return city;
    }

    String state() {
        return state;
    }

    Double latitude() {
        return latitude == null ? null : latitude.doubleValue();
    }

    Double longitude() {
        return longitude == null ? null : longitude.doubleValue();
    }

    String phoneNumber() {
        return phoneNumber;
    }

    String whatsappNumber() {
        return whatsappNumber;
    }

    String logoUrl() {
        return logoUrl;
    }

    BigDecimal rating() {
        return rating;
    }

    int reviewCount() {
        return reviewCount;
    }

    int patientCount() {
        return patientCount;
    }

    boolean verified() {
        return verified;
    }

    List<OperatingHours> hours() {
        return hours;
    }
}
