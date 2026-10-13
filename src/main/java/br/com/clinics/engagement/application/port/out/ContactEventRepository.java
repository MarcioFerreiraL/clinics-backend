package br.com.clinics.engagement.application.port.out;

import br.com.clinics.engagement.domain.ContactEvent;

public interface ContactEventRepository {

    void save(ContactEvent event);
}
