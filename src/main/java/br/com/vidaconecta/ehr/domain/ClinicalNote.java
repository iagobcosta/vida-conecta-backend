package br.com.vidaconecta.ehr.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "clinical_notes")
public class ClinicalNote {

	@Id
	private UUID id;

	@Column(name = "patient_id", nullable = false)
	private UUID patientId;

	@Column(name = "author_doctor_id", nullable = false)
	private UUID authorDoctorId;

	@Column(name = "appointment_id")
	private UUID appointmentId;

	@Column(nullable = false, columnDefinition = "bytea")
	private byte[] ciphertext;

	@Column(nullable = false, columnDefinition = "bytea")
	private byte[] iv;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected ClinicalNote() {
	}

	public static ClinicalNote create(
			UUID patientId,
			UUID authorDoctorId,
			UUID appointmentId,
			byte[] ciphertext,
			byte[] iv) {
		ClinicalNote note = new ClinicalNote();
		note.id = UUID.randomUUID();
		note.patientId = patientId;
		note.authorDoctorId = authorDoctorId;
		note.appointmentId = appointmentId;
		note.ciphertext = ciphertext;
		note.iv = iv;
		note.createdAt = Instant.now();
		return note;
	}

	public UUID getId() {
		return id;
	}

	public UUID getPatientId() {
		return patientId;
	}

	public UUID getAuthorDoctorId() {
		return authorDoctorId;
	}

	public UUID getAppointmentId() {
		return appointmentId;
	}

	public byte[] getCiphertext() {
		return ciphertext;
	}

	public byte[] getIv() {
		return iv;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public boolean authoredBy(UUID doctorId) {
		return authorDoctorId.equals(doctorId);
	}
}
