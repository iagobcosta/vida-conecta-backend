package br.com.vidaconecta.identity.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record RegisterAdminRequest(
		@NotNull UUID token,
		@NotBlank @Email String email,
		@NotBlank @Size(min = 8) String password,
		@NotBlank String fullName) {
}
