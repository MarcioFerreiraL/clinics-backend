package br.com.clinics.model;

import br.com.clinics.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CpfTest {

    @Test
    void normalizesFormattedCpfToDigits() {
        assertThat(Cpf.of("529.982.247-25").digits()).isEqualTo("52998224725");
    }

    @Test
    void formatsForDisplay() {
        assertThat(Cpf.of("52998224725").formatted()).isEqualTo("529.982.247-25");
    }

    @ParameterizedTest
    @ValueSource(strings = {"52998224724", "11111111111", "123", "abc", "529982247250"})
    void rejectsInvalidCpf(String raw) {
        assertThatThrownBy(() -> Cpf.of(raw))
                .isInstanceOf(ValidationException.class)
                .extracting("code").isEqualTo("INVALID_CPF");
    }

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> Cpf.of(null)).isInstanceOf(ValidationException.class);
    }
}
