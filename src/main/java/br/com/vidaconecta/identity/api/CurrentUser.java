package br.com.vidaconecta.identity.api;

import java.util.UUID;

public record CurrentUser(UUID id, String email, Role role) {

	public boolean isPatient() {
		return role == Role.PACIENTE;
	}

	public boolean isDoctor() {
		return role == Role.MEDICO;
	}

	public boolean isAdmin() {
		return role == Role.ADMIN;
	}
}
