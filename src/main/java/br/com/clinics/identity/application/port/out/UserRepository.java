package br.com.clinics.identity.application.port.out;

import java.util.Optional;
import java.util.UUID;

import br.com.clinics.identity.domain.Cpf;
import br.com.clinics.identity.domain.Phone;
import br.com.clinics.identity.domain.User;

public interface UserRepository {

    Optional<User> findByPhone(Phone phone);

    boolean existsByPhone(Phone phone);

    boolean existsByCpf(Cpf cpf);

    Optional<User> findById(UUID id);

    /** Verdadeiro se outro usuário (diferente de {@code excludedId}) já usa o e-mail, ignorando caixa (RN04). */
    boolean existsByEmailExcluding(String email, UUID excludedId);

    User save(User user);
}
