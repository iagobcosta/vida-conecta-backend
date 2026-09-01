package br.com.vidaconecta.prescription.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record CreatePrescriptionRequest(
		@NotNull UUID patientId,
		@NotNull UUID appointmentId,
		@NotEmpty List<@Valid ItemRequest> items) {

	public record ItemRequest(
			@NotBlank String medication,
			@NotBlank String dosage,
			@NotBlank String instructions) {
	}
}
