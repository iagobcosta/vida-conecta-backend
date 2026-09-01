package br.com.vidaconecta.notification.infrastructure;

import br.com.vidaconecta.notification.domain.Notification;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

	List<Notification> findByRecipientIdOrderByCreatedAtDesc(UUID recipientId);

	long countByRecipientIdAndReadAtIsNull(UUID recipientId);

	List<Notification> findByRecipientIdAndReadAtIsNull(UUID recipientId);
}
