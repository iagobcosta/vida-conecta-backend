package br.com.vidaconecta.scheduling.web;

import jakarta.validation.constraints.Size;

public record CancelAppointmentRequest(
		@Size(max = 500) String reason) {
}
