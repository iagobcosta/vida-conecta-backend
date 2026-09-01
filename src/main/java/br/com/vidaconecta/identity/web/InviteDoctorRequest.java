package br.com.vidaconecta.identity.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record InviteDoctorRequest(
		@NotBlank @Email String email,
		@NotBlank String fullName) {
}
