package br.com.vidaconecta.prescription.web;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PrescriptionResponse(
		UUID id,
		UUID patientId,
		UUID doctorId,
		String doctorName,
		UUID appointmentId,
		Instant issuedAt,
		List<PrescriptionItemResponse> items) {
}
