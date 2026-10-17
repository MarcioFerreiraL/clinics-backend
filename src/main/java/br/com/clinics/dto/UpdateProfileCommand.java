package br.com.clinics.dto;

import java.time.LocalDate;
import java.util.UUID;
import br.com.clinics.model.User;

public record UpdateProfileCommand(UUID userId, String fullName, String email, LocalDate birthDate) {
}
