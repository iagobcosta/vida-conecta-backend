package br.com.vidaconecta.scheduling.web;

import java.util.UUID;

public record DoctorResponse(UUID id, String fullName, String crm, String specialty) {
}
