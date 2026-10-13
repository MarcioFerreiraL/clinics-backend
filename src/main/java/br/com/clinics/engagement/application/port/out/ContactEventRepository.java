package br.com.clinics.engagement.application.port.out;

import java.util.UUID;

import br.com.clinics.engagement.domain.ContactEvent;
import br.com.clinics.engagement.domain.SavingsSummary;

public interface ContactEventRepository {

    void save(ContactEvent event);

    /** Soma a economia e conta os contatos do usuário; contatos de visitantes não entram (RN31). */
    SavingsSummary summarizeByUser(UUID userId);
}
