package br.com.vidaconecta.consent.web;

import br.com.vidaconecta.consent.api.ConsentScope;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record GrantConsentRequest(
		@NotNull UUID doctorId,
		@NotNull ConsentScope scope,
		UUID appointmentId,
		Instant expiresAt) {
}
