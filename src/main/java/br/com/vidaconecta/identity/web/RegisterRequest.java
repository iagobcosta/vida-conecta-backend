package br.com.vidaconecta.identity.web;

import br.com.vidaconecta.identity.api.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record RegisterRequest(
		@NotBlank @Email String email,
		@NotBlank @Size(min = 8) String password,
		@NotNull Role role,
		@NotBlank String fullName,
		String cpf,
		LocalDate birthDate,
		String phone,
		String crm,
		String specialty) {
}
