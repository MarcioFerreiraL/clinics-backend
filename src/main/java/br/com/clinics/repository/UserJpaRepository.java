package br.com.clinics.repository;

import br.com.clinics.model.UserJpaEntity;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {

    Optional<UserJpaEntity> findByPhone(String phone);

    boolean existsByPhone(String phone);

    boolean existsByCpf(String cpf);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);
}
