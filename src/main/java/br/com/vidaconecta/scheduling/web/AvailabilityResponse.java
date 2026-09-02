package br.com.vidaconecta.scheduling.web;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

public record AvailabilityResponse(
		UUID id,
		UUID doctorId,
		DayOfWeek dayOfWeek,
		LocalTime startTime,
		LocalTime endTime,
		int slotMinutes) {
}
