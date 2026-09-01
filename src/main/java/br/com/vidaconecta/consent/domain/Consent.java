package br.com.vidaconecta.consent.domain;

import br.com.vidaconecta.consent.api.ConsentScope;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "consents")
public class Consent {

	@Id
	private UUID id;

	@Column(name = "patient_id", nullable = false)
	private UUID patientId;

	@Column(name = "doctor_id", nullable = false)
	private UUID doctorId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ConsentScope scope;

	@Column(name = "appointment_id")
	private UUID appointmentId;

	@Column(nullable = false)
	private int version;

	@Column(name = "granted_at", nullable = false)
	private Instant grantedAt;

	@Column(name = "expires_at")
	private Instant expiresAt;

	@Column(name = "revoked_at")
	private Instant revokedAt;

	protected Consent() {
	}

	public static Consent grant(
			UUID patientId,
			UUID doctorId,
			ConsentScope scope,
			UUID appointmentId,
			int version,
			Instant expiresAt) {
		Consent consent = new Consent();
		consent.id = UUID.randomUUID();
		consent.patientId = patientId;
		consent.doctorId = doctorId;
		consent.scope = scope;
		consent.appointmentId = appointmentId;
		consent.version = version;
		consent.grantedAt = Instant.now();
		consent.expiresAt = expiresAt;
		return consent;
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

	public ConsentScope getScope() {
		return scope;
	}

	public UUID getAppointmentId() {
		return appointmentId;
	}

	public int getVersion() {
		return version;
	}

	public Instant getGrantedAt() {
		return grantedAt;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public Instant getRevokedAt() {
		return revokedAt;
	}

	public boolean isActive(Instant now) {
		if (revokedAt != null) {
			return false;
		}
		return expiresAt == null || expiresAt.isAfter(now);
	}

	public void revoke() {
		if (revokedAt != null) {
			throw new IllegalStateException("Consentimento já revogado");
		}
		revokedAt = Instant.now();
	}

	public boolean covers(UUID doctorId, UUID appointmentId) {
		if (!this.doctorId.equals(doctorId)) {
			return false;
		}
		if (scope == ConsentScope.DOCTOR) {
			return true;
		}
		return appointmentId != null && appointmentId.equals(this.appointmentId);
	}
}
