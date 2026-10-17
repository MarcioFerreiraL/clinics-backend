package br.com.clinics.dto;

import java.time.LocalDate;
import br.com.clinics.dto.AuthResult;

public record RegisterUserCommand(String fullName, String phone, String cpf, String email, LocalDate birthDate,
        String password) {
}
