package br.com.vidaconecta.notification.application;

import br.com.vidaconecta.identity.api.CurrentUser;
import br.com.vidaconecta.notification.api.NotificationFacade;
import br.com.vidaconecta.notification.domain.Notification;
import br.com.vidaconecta.notification.infrastructure.NotificationRepository;
import br.com.vidaconecta.notification.web.NotificationResponse;
import br.com.vidaconecta.notification.web.UnreadCountResponse;
import br.com.vidaconecta.shared.api.ForbiddenException;
import br.com.vidaconecta.shared.api.NotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

	private final NotificationRepository notificationRepository;

	public NotificationService(NotificationRepository notificationRepository) {
		this.notificationRepository = notificationRepository;
	}

	@Transactional
	public void create(NotificationFacade.NewNotification command) {
		notificationRepository.save(Notification.create(
				command.recipientId(),
				command.type(),
				command.title(),
				command.body(),
				command.appointmentId(),
				command.actionPath(),
				command.actionLabel()));
	}

	@Transactional(readOnly = true)
	public List<NotificationResponse> list(CurrentUser currentUser) {
		return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(currentUser.id()).stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public UnreadCountResponse unreadCount(CurrentUser currentUser) {
		return new UnreadCountResponse(notificationRepository.countByRecipientIdAndReadAtIsNull(currentUser.id()));
	}

	@Transactional
	public NotificationResponse markRead(CurrentUser currentUser, UUID notificationId) {
		Notification notification = notificationRepository.findById(notificationId)
				.orElseThrow(() -> new NotFoundException("Notificação não encontrada"));
		if (!notification.getRecipientId().equals(currentUser.id())) {
			throw new ForbiddenException("Você não pode alterar esta notificação");
		}
		notification.markRead();
		return toResponse(notification);
	}

	@Transactional
	public void markAllRead(CurrentUser currentUser) {
		notificationRepository.findByRecipientIdAndReadAtIsNull(currentUser.id())
				.forEach(Notification::markRead);
	}

	private NotificationResponse toResponse(Notification notification) {
		return new NotificationResponse(
				notification.getId(),
				notification.getType(),
				notification.getTitle(),
				notification.getBody(),
				notification.getAppointmentId(),
				notification.getActionPath(),
				notification.getActionLabel(),
				notification.getReadAt(),
				notification.getCreatedAt());
	}
}
