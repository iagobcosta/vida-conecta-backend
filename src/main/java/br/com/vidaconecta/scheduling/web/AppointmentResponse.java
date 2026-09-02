package br.com.vidaconecta.scheduling.web;

import br.com.vidaconecta.scheduling.api.AppointmentStatus;
import java.time.Instant;
import java.util.UUID;

public record AppointmentResponse(
		UUID id,
		UUID patientId,
		String patientName,
		UUID doctorId,
		String doctorName,
		Instant scheduledAt,
		int durationMinutes,
		AppointmentStatus status,
		Instant joinOpensAt,
		Instant joinClosesAt,
		boolean canJoinNow,
		String cancelReason,
		UUID cancelledBy,
		String cancelledByName) {
}
