package br.com.vidaconecta.notification.application;

import br.com.vidaconecta.notification.api.NotificationFacade;
import org.springframework.stereotype.Service;

@Service
public class NotificationFacadeImpl implements NotificationFacade {

	private final NotificationService notificationService;

	public NotificationFacadeImpl(NotificationService notificationService) {
		this.notificationService = notificationService;
	}

	@Override
	public void push(NewNotification notification) {
		notificationService.create(notification);
	}
}
