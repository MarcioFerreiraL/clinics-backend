package br.com.clinics.clinicadmin;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, UUID> {
}
