package br.com.vidaconecta.ehr.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateClinicalNoteRequest(@NotNull UUID appointmentId, @NotBlank String content) {
}
