package br.com.clinics.engagement.application.port.out;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/** Porta anticorrupção: o que o módulo engagement precisa saber de uma oferta, sem depender do modelo do catálogo. */
public interface ContactableExamLookup {

    Optional<ContactableExam> findById(UUID examId);

    record ContactableExam(UUID examId, String examName, BigDecimal price, BigDecimal savings, String whatsappNumber) {
    }
}
