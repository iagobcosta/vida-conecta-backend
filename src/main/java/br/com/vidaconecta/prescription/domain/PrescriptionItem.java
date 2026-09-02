package br.com.vidaconecta.prescription.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "prescription_items")
public class PrescriptionItem {

	@Id
	private UUID id;

	@Column(nullable = false)
	private String medication;

	@Column(nullable = false)
	private String dosage;

	@Column(nullable = false)
	private String instructions;

	protected PrescriptionItem() {
	}

	public static PrescriptionItem of(String medication, String dosage, String instructions) {
		PrescriptionItem item = new PrescriptionItem();
		item.id = UUID.randomUUID();
		item.medication = medication;
		item.dosage = dosage;
		item.instructions = instructions;
		return item;
	}

	public UUID getId() {
		return id;
	}

	public String getMedication() {
		return medication;
	}

	public String getDosage() {
		return dosage;
	}

	public String getInstructions() {
		return instructions;
	}
}
