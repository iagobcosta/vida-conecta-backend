package br.com.vidaconecta.identity.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CompleteDoctorRequest(
		@NotNull UUID token,
		@NotBlank @Size(min = 8) String password,
		@NotBlank String crm,
		@NotBlank String specialty) {
}
