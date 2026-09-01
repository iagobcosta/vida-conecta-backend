package br.com.vidaconecta.consent.web;

import br.com.vidaconecta.consent.api.ConsentScope;
import java.time.Instant;
import java.util.UUID;

public record ConsentResponse(
		UUID id,
		UUID patientId,
		String patientName,
		UUID doctorId,
		String doctorName,
		ConsentScope scope,
		UUID appointmentId,
		int version,
		Instant grantedAt,
		Instant expiresAt,
		Instant revokedAt) {
}
