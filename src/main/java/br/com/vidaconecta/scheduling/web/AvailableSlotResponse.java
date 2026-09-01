package br.com.vidaconecta.scheduling.web;

import java.time.Instant;

public record AvailableSlotResponse(Instant startAt, int durationMinutes) {
}
