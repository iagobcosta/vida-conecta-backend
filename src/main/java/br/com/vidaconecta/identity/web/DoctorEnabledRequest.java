package br.com.vidaconecta.identity.web;

import jakarta.validation.constraints.NotNull;

public record DoctorEnabledRequest(@NotNull Boolean enabled) {
}
