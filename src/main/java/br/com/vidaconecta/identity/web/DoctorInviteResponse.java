package br.com.vidaconecta.identity.web;

import java.time.Instant;
import java.util.UUID;

public record DoctorInviteResponse(
		UUID id,
		String email,
		String fullName,
		UUID token,
		String inviteUrl,
		String status,
		Instant expiresAt,
		Instant acceptedAt,
		Instant createdAt) {
}
