package br.com.vidaconecta.identity.web;

import java.util.UUID;

public record ManagedDoctorResponse(
		UUID id,
		String email,
		String fullName,
		String crm,
		String specialty,
		boolean enabled) {
}
