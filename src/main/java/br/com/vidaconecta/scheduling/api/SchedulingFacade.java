package br.com.vidaconecta.scheduling.api;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface SchedulingFacade {

	Optional<AppointmentView> findById(UUID appointmentId);

	boolean isParticipant(UUID appointmentId, UUID userId);

	boolean hasRelationship(UUID patientId, UUID doctorId);

	boolean canJoinVideo(UUID appointmentId, UUID userId, Instant now);

	record AppointmentView(
			UUID id,
			UUID patientId,
			UUID doctorId,
			Instant scheduledAt,
			int durationMinutes,
			AppointmentStatus status) {

		public Instant endsAt() {
			return scheduledAt.plusSeconds(durationMinutes * 60L);
		}
	}
}
