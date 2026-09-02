package br.com.vidaconecta.prescription.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "prescriptions")
public class Prescription {

	@Id
	private UUID id;

	@Column(name = "patient_id", nullable = false)
	private UUID patientId;

	@Column(name = "doctor_id", nullable = false)
	private UUID doctorId;

	@Column(name = "appointment_id", nullable = false)
	private UUID appointmentId;

	@Column(name = "issued_at", nullable = false)
	private Instant issuedAt;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "prescription_id", nullable = false)
	private List<PrescriptionItem> items = new ArrayList<>();

	protected Prescription() {
	}

	public static Prescription issue(UUID patientId, UUID doctorId, UUID appointmentId, List<PrescriptionItem> items) {
		Prescription prescription = new Prescription();
		prescription.id = UUID.randomUUID();
		prescription.patientId = patientId;
		prescription.doctorId = doctorId;
		prescription.appointmentId = appointmentId;
		prescription.issuedAt = Instant.now();
		prescription.items.addAll(items);
		return prescription;
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

	public UUID getAppointmentId() {
		return appointmentId;
	}

	public Instant getIssuedAt() {
		return issuedAt;
	}

	public List<PrescriptionItem> getItems() {
		return items;
	}

	public boolean isVisibleTo(UUID userId) {
		return patientId.equals(userId) || doctorId.equals(userId);
	}
}
