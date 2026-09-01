package br.com.vidaconecta.notification.domain;

import br.com.vidaconecta.notification.api.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification {

	@Id
	private UUID id;

	@Column(name = "recipient_id", nullable = false)
	private UUID recipientId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 64)
	private NotificationType type;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(nullable = false, length = 1000)
	private String body;

	@Column(name = "appointment_id")
	private UUID appointmentId;

	@Column(name = "action_path", length = 200)
	private String actionPath;

	@Column(name = "action_label", length = 80)
	private String actionLabel;

	@Column(name = "read_at")
	private Instant readAt;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected Notification() {
	}

	public static Notification create(
			UUID recipientId,
			NotificationType type,
			String title,
			String body,
			UUID appointmentId,
			String actionPath,
			String actionLabel) {
		Notification notification = new Notification();
		notification.id = UUID.randomUUID();
		notification.recipientId = recipientId;
		notification.type = type;
		notification.title = title;
		notification.body = body;
		notification.appointmentId = appointmentId;
		notification.actionPath = actionPath;
		notification.actionLabel = actionLabel;
		notification.createdAt = Instant.now();
		return notification;
	}

	public UUID getId() {
		return id;
	}

	public UUID getRecipientId() {
		return recipientId;
	}

	public NotificationType getType() {
		return type;
	}

	public String getTitle() {
		return title;
	}

	public String getBody() {
		return body;
	}

	public UUID getAppointmentId() {
		return appointmentId;
	}

	public String getActionPath() {
		return actionPath;
	}

	public String getActionLabel() {
		return actionLabel;
	}

	public Instant getReadAt() {
		return readAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public boolean isUnread() {
		return readAt == null;
	}

	public void markRead() {
		if (readAt == null) {
			readAt = Instant.now();
		}
	}
}
