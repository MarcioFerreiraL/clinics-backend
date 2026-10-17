package br.com.clinics.model;

import java.math.BigDecimal;

import br.com.clinics.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WhatsAppLinkBuilderTest {

    @Test
    void buildsLinkWithDigitsOnlyAndEncodedMessage() {
        String url = WhatsAppLinkBuilder.build("+55 (81) 99999-9999", "Hemograma Completo", new BigDecimal("18")).toString();

        assertThat(url).startsWith("https://wa.me/5581999999999?text=");
        assertThat(url).contains("Ol%C3%A1%21%20Vi%20o%20exame%20Hemograma%20Completo%20por%20R%24%2018%2C00%20no%20Clinics");
        assertThat(url).doesNotContain(" ").doesNotContain("+");
    }

    @Test
    void encodesSpecialCharactersOfTheExamName() {
        String url = WhatsAppLinkBuilder.build("5581999999999", "Raio-X & Tórax", new BigDecimal("55.5")).toString();
        assertThat(url).contains("Raio-X%20%26%20T%C3%B3rax").contains("55%2C50");
    }

    @Test
    void rejectsClinicWithoutWhatsapp() {
        assertThatThrownBy(() -> WhatsAppLinkBuilder.build(" ", "Exame", BigDecimal.TEN))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void contactEventNeverStoresNegativeSavings() {
        ContactEvent event = ContactEvent.of(java.util.UUID.randomUUID(), null, BigDecimal.TEN, new BigDecimal("-5"),
                java.time.Instant.now());
        assertThat(event.estimatedSavings()).isEqualByComparingTo("0");
    }
}
