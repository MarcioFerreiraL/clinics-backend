package br.com.clinics.repository;

import br.com.clinics.model.UserJpaEntity;

import java.util.Optional;
import java.util.UUID;

import br.com.clinics.repository.UserRepository;
import br.com.clinics.model.Cpf;
import br.com.clinics.model.Phone;
import br.com.clinics.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserRepository {

    private final UserJpaRepository jpa;

    public UserRepository(UserJpaRepository jpa) {
        this.jpa = jpa;
    }
    public Optional<User> findByPhone(Phone phone) {
        return jpa.findByPhone(phone.digits()).map(this::toDomain);
    }
    public boolean existsByPhone(Phone phone) {
        return jpa.existsByPhone(phone.digits());
    }
    public boolean existsByCpf(Cpf cpf) {
        return jpa.existsByCpf(cpf.digits());
    }
    public Optional<User> findById(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }
    public boolean existsByEmailExcluding(String email, UUID excludedId) {
        return jpa.existsByEmailIgnoreCaseAndIdNot(email, excludedId);
    }
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
