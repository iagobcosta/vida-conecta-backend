package br.com.vidaconecta.ehr.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ehr_access_audit")
public class EhrAccessAudit {

	@Id
	private UUID id;

	@Column(name = "actor_user_id", nullable = false)
	private UUID actorUserId;

	@Column(name = "patient_id", nullable = false)
	private UUID patientId;

	@Column(name = "appointment_id")
	private UUID appointmentId;

	@Column(nullable = false)
	private String action;

	@Column(name = "accessed_at", nullable = false)
	private Instant accessedAt;

	protected EhrAccessAudit() {
	}

	public static EhrAccessAudit record(UUID actorUserId, UUID patientId, UUID appointmentId, String action) {
		EhrAccessAudit audit = new EhrAccessAudit();
		audit.id = UUID.randomUUID();
		audit.actorUserId = actorUserId;
		audit.patientId = patientId;
		audit.appointmentId = appointmentId;
		audit.action = action;
		audit.accessedAt = Instant.now();
		return audit;
	}

	public UUID getId() {
		return id;
	}

	public UUID getActorUserId() {
		return actorUserId;
	}

	public UUID getPatientId() {
		return patientId;
	}

	public UUID getAppointmentId() {
		return appointmentId;
	}

	public String getAction() {
		return action;
	}

	public Instant getAccessedAt() {
		return accessedAt;
	}
}
