package br.com.clinics.identity.domain;

import java.time.Instant;
import java.time.LocalDate;

import br.com.clinics.shared.domain.ValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserProfileTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 12);
    private static final Instant NOW = Instant.parse("2026-10-12T12:00:00Z");

    private User newUser() {
        return User.register(Phone.of("81998765432"), Cpf.of("529.982.247-25"), "Maria Silva", "maria@email.com",
                LocalDate.of(1990, 5, 20), "hash", TODAY, Instant.parse("2026-01-01T00:00:00Z"));
    }

    @Test
    void updatesEditableFieldsAndKeepsPhone() {
        User user = newUser();
        user.updateProfile("  Maria de Souza  ", " Maria@Email.COM ", LocalDate.of(1996, 8, 14), TODAY, NOW);

        assertThat(user.fullName()).isEqualTo("Maria de Souza");
        assertThat(user.email()).isEqualTo("maria@email.com");
        assertThat(user.birthDate()).isEqualTo(LocalDate.of(1996, 8, 14));
        assertThat(user.phone().digits()).isEqualTo("5581998765432");
        assertThat(user.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void blankEmailClearsIt() {
        User user = newUser();
        user.updateProfile("Maria", "a@b.com", null, TODAY, NOW);
        user.updateProfile("Maria", "  ", null, TODAY, NOW);
        assertThat(user.email()).isNull();
    }

    @Test
    void rejectsInvalidData() {
        User user = newUser();
        assertThatThrownBy(() -> user.updateProfile(" ", null, null, TODAY, NOW)).hasFieldOrPropertyWithValue("code", "INVALID_NAME");
        assertThatThrownBy(() -> user.updateProfile("Maria", "sem-arroba", null, TODAY, NOW)).hasFieldOrPropertyWithValue("code", "INVALID_EMAIL");
        assertThatThrownBy(() -> user.updateProfile("Maria", null, TODAY.plusDays(1), TODAY, NOW)).hasFieldOrPropertyWithValue("code", "INVALID_BIRTH_DATE");
    }

    @Test
    void acceptsBirthDateToday() {
        User user = newUser();
        user.updateProfile("Maria", null, TODAY, TODAY, NOW);
        assertThat(user.birthDate()).isEqualTo(TODAY);
    }

    @Test
    void failedUpdateLeavesTheUserUntouched() {
        User user = newUser();
        assertThatThrownBy(() -> user.updateProfile("Novo Nome", "invalido", null, TODAY, NOW)).isInstanceOf(ValidationException.class);
        assertThat(user.fullName()).isEqualTo("Maria Silva");
    }
}
