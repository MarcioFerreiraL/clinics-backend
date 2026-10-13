package br.com.clinics.engagement.application.port.in;

import java.util.UUID;

import br.com.clinics.engagement.domain.SavingsSummary;

/** UC08 · Ver economia. */
public interface GetSavingsUseCase {

    SavingsSummary getSavings(UUID userId);
}
