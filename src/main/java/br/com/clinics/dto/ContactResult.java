package br.com.clinics.dto;

import java.math.BigDecimal;
import java.net.URI;
import java.util.UUID;

public record ContactResult(UUID contactId, URI whatsappUrl, BigDecimal estimatedSavings) {
}
