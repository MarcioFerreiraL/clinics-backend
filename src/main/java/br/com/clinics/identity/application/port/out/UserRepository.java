package br.com.clinics.identity.application.port.out;

import java.util.Optional;

import br.com.clinics.identity.domain.Phone;
import br.com.clinics.identity.domain.User;

public interface UserRepository {

    Optional<User> findByPhone(Phone phone);

    boolean existsByPhone(Phone phone);

    User save(User user);
}
