package br.com.vidaconecta.identity.web;

import java.time.LocalDate;

public record UpdateProfileRequest(
		String fullName,
		String phone,
		LocalDate birthDate,
		String specialty
) {}
