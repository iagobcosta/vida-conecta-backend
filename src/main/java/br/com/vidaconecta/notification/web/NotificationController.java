package br.com.vidaconecta.notification.web;

import br.com.vidaconecta.identity.api.CurrentUser;
import br.com.vidaconecta.notification.application.NotificationService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

	private final NotificationService notificationService;

	public NotificationController(NotificationService notificationService) {
		this.notificationService = notificationService;
	}

	@GetMapping
	public List<NotificationResponse> list(@AuthenticationPrincipal CurrentUser currentUser) {
		return notificationService.list(currentUser);
	}

	@GetMapping("/unread-count")
	public UnreadCountResponse unreadCount(@AuthenticationPrincipal CurrentUser currentUser) {
		return notificationService.unreadCount(currentUser);
	}

	@PostMapping("/{id}/read")
	public NotificationResponse markRead(
			@AuthenticationPrincipal CurrentUser currentUser,
			@PathVariable UUID id) {
		return notificationService.markRead(currentUser, id);
	}

	@PostMapping("/read-all")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void markAllRead(@AuthenticationPrincipal CurrentUser currentUser) {
		notificationService.markAllRead(currentUser);
	}
}
