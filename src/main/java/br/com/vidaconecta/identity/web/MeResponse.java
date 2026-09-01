package br.com.vidaconecta.identity.web;

import br.com.vidaconecta.identity.api.Role;
import br.com.vidaconecta.identity.domain.DoctorProfile;
import br.com.vidaconecta.identity.domain.PatientProfile;
import br.com.vidaconecta.identity.domain.User;
import java.time.LocalDate;
import java.util.UUID;

public record MeResponse(
		UUID id,
		String email,
		Role role,
		String fullName,
		String cpf,
		LocalDate birthDate,
		String phone,
		String crm,
		String specialty) {

	public static MeResponse patient(User user, PatientProfile profile) {
		return new MeResponse(
				user.getId(),
				user.getEmail(),
				user.getRole(),
				profile.getFullName(),
				profile.getCpf(),
				profile.getBirthDate(),
				profile.getPhone(),
				null,
				null);
	}

	public static MeResponse doctor(User user, DoctorProfile profile) {
		return new MeResponse(
				user.getId(),
				user.getEmail(),
				user.getRole(),
				profile.getFullName(),
				null,
				null,
				null,
				profile.getCrm(),
				profile.getSpecialty());
	}

	public static MeResponse admin(User user) {
		return new MeResponse(user.getId(), user.getEmail(), user.getRole(), null, null, null, null, null, null);
	}
}
