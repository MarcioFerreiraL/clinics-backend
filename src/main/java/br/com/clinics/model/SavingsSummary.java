package br.com.clinics.model;

import java.math.BigDecimal;

/** Total economizado pelo paciente: soma da economia congelada em cada contato (RN31). */
public record SavingsSummary(BigDecimal totalSavings, long contactsCount) {

    public static SavingsSummary empty() {
        return new SavingsSummary(BigDecimal.ZERO.setScale(2), 0);
    }
}
