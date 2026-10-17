package br.com.clinics.dto;

import br.com.clinics.dto.AuthResult;

public record AuthenticateCommand(String phone, String password) {
}
