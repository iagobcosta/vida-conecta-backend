package br.com.vidaconecta.video.web;

import br.com.vidaconecta.video.api.VideoSessionEvent;
import jakarta.validation.constraints.NotNull;

public record VideoSessionEventRequest(@NotNull VideoSessionEvent event) {
}