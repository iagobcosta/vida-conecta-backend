package br.com.vidaconecta.scheduling.web;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record CreateAppointmentRequest(
		@NotNull UUID doctorId,
		@NotNull @Future Instant scheduledAt,
		Integer durationMinutes) {
}
