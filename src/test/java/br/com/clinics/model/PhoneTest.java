package br.com.clinics.model;

import br.com.clinics.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhoneTest {

    @ParameterizedTest
    @ValueSource(strings = {"(81) 99876-5432", "81998765432", "5581998765432", "+55 81 99876-5432"})
    void normalizesToDigitsWithCountryCode(String raw) {
        assertThat(Phone.of(raw).digits()).isEqualTo("5581998765432");
    }

    @Test
    void formatsForDisplay() {
        assertThat(Phone.of("81998765432").formatted()).isEqualTo("(81) 99876-5432");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "123", "(81) 3634-1122", "81898765432", "(01) 99876-5432", "819987654321"})
    void rejectsInvalidNumbers(String raw) {
        assertThatThrownBy(() -> Phone.of(raw))
                .isInstanceOf(ValidationException.class)
                .extracting("code").isEqualTo("INVALID_PHONE");
    }
}
