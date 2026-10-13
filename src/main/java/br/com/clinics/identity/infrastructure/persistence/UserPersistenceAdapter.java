package br.com.clinics.identity.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import br.com.clinics.identity.application.port.out.UserRepository;
import br.com.clinics.identity.domain.Cpf;
import br.com.clinics.identity.domain.Phone;
import br.com.clinics.identity.domain.User;
import org.springframework.stereotype.Component;

@Component
class UserPersistenceAdapter implements UserRepository {

    private final SpringDataUserRepository jpa;

    UserPersistenceAdapter(SpringDataUserRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<User> findByPhone(Phone phone) {
        return jpa.findByPhone(phone.digits()).map(this::toDomain);
    }

    @Override
    public boolean existsByPhone(Phone phone) {
        return jpa.existsByPhone(phone.digits());
    }

    @Override
    public boolean existsByCpf(Cpf cpf) {
        return jpa.existsByCpf(cpf.digits());
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public boolean existsByEmailExcluding(String email, UUID excludedId) {
        return jpa.existsByEmailIgnoreCaseAndIdNot(email, excludedId);
    }

    @Override
    public User save(User user) {
        return toDomain(jpa.save(toEntity(user)));
    }

    private User toDomain(UserJpaEntity e) {
        return new User(e.getId(), new Phone(e.getPhone()), e.getCpf() == null ? null : new Cpf(e.getCpf()), e.getEmail(), e.getPasswordHash(), e.getFullName(),
                e.getRole(), e.getBirthDate(), e.getAvatarUrl(), e.getCreatedAt(), e.getUpdatedAt());
    }

    private UserJpaEntity toEntity(User u) {
        return new UserJpaEntity(u.id(), u.phone().digits(), u.cpf() == null ? null : u.cpf().digits(), u.email(), u.passwordHash(), u.fullName(), u.role(),
                u.birthDate(), u.avatarUrl(), u.createdAt(), u.updatedAt());
    }
}
