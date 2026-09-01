package br.com.vidaconecta.notification.web;

import br.com.vidaconecta.notification.api.NotificationType;
import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
		UUID id,
		NotificationType type,
		String title,
		String body,
		UUID appointmentId,
		String actionPath,
		String actionLabel,
		Instant readAt,
		Instant createdAt) {
}
