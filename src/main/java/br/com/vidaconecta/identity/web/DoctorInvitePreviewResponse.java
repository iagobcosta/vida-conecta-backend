package br.com.vidaconecta.identity.web;

import java.time.Instant;

public record DoctorInvitePreviewResponse(String email, String fullName, Instant expiresAt) {
}
