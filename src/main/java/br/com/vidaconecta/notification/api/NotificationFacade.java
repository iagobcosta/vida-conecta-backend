package br.com.vidaconecta.notification.api;

import java.util.UUID;

public interface NotificationFacade {

	void push(NewNotification notification);

	record NewNotification(
			UUID recipientId,
			NotificationType type,
			String title,
			String body,
			UUID appointmentId,
			String actionPath,
			String actionLabel) {
	}
}
