package br.com.vidaconecta.scheduling.domain;

import br.com.vidaconecta.scheduling.api.AppointmentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "appointments")
public class Appointment {

	@Id
	private UUID id;

	@Column(name = "patient_id", nullable = false)
	private UUID patientId;

	@Column(name = "doctor_id", nullable = false)
	private UUID doctorId;

	@Column(name = "scheduled_at", nullable = false)
	private Instant scheduledAt;

	@Column(name = "duration_minutes", nullable = false)
	private int durationMinutes;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private AppointmentStatus status;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Appointment() {
	}

	public static Appointment schedule(UUID patientId, UUID doctorId, Instant scheduledAt, int durationMinutes) {
		Appointment appointment = new Appointment();
		appointment.id = UUID.randomUUID();
		appointment.patientId = patientId;
		appointment.doctorId = doctorId;
		appointment.scheduledAt = scheduledAt;
		appointment.durationMinutes = durationMinutes;
		appointment.status = AppointmentStatus.SCHEDULED;
		Instant now = Instant.now();
		appointment.createdAt = now;
		appointment.updatedAt = now;
		return appointment;
	}

	public UUID getId() {
		return id;
	}

	public UUID getPatientId() {
		return patientId;
	}

	public UUID getDoctorId() {
		return doctorId;
	}

	public Instant getScheduledAt() {
		return scheduledAt;
	}

	public int getDurationMinutes() {
		return durationMinutes;
	}

	public AppointmentStatus getStatus() {
		return status;
	}

	public Instant endsAt() {
		return scheduledAt.plusSeconds(durationMinutes * 60L);
	}

	public boolean isParticipant(UUID userId) {
		return patientId.equals(userId) || doctorId.equals(userId);
	}

	public boolean overlaps(Instant otherStart, Instant otherEnd) {
		return scheduledAt.isBefore(otherEnd) && otherStart.isBefore(endsAt());
	}

	public void confirm() {
		ensureActive();
		if (status != AppointmentStatus.SCHEDULED) {
			throw new IllegalStateException("Somente consultas agendadas podem ser confirmadas");
		}
		status = AppointmentStatus.CONFIRMED;
		updatedAt = Instant.now();
	}

	public void cancel() {
		ensureActive();
		status = AppointmentStatus.CANCELLED;
		updatedAt = Instant.now();
	}

	public void markInProgress() {
		if (status == AppointmentStatus.CONFIRMED) {
			status = AppointmentStatus.IN_PROGRESS;
			updatedAt = Instant.now();
		}
	}

	public boolean isJoinable() {
		return status == AppointmentStatus.CONFIRMED || status == AppointmentStatus.IN_PROGRESS;
	}

	private void ensureActive() {
		if (status == AppointmentStatus.CANCELLED || status == AppointmentStatus.COMPLETED) {
			throw new IllegalStateException("Consulta encerrada não pode ser alterada");
		}
	}
}
