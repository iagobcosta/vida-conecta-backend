package br.com.vidaconecta.ehr.web;

import java.time.Instant;
import java.util.UUID;

public record EhrAuditResponse(
		UUID id,
		UUID actorUserId,
		UUID patientId,
		UUID appointmentId,
		String action,
		Instant accessedAt) {
}
