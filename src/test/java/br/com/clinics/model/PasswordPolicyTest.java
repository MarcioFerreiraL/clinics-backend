package br.com.clinics.model;

import br.com.clinics.exception.ValidationException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyTest {

    @ParameterizedTest
    @ValueSource(strings = {"senha123", "Abc12345", "umaSenhaLonga9"})
    void acceptsStrongPasswords(String password) {
        assertThatCode(() -> PasswordPolicy.validate(password)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "abc123", "somenteletras", "12345678"})
    void rejectsWeakPasswords(String password) {
        assertThatThrownBy(() -> PasswordPolicy.validate(password))
                .isInstanceOf(ValidationException.class)
                .extracting("code").isEqualTo("WEAK_PASSWORD");
    }
}
