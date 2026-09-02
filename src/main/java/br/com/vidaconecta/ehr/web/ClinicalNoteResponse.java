package br.com.vidaconecta.ehr.web;

import java.time.Instant;
import java.util.UUID;

public record ClinicalNoteResponse(
		UUID id,
		UUID patientId,
		UUID authorDoctorId,
		String authorName,
		UUID appointmentId,
		String content,
		Instant createdAt) {
}
