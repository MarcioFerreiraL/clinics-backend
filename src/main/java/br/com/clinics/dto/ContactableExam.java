package br.com.clinics.dto;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public record ContactableExam(UUID examId, String examName, BigDecimal price, BigDecimal savings, String whatsappNumber) {
}
