package br.com.vidaconecta.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "patient_profiles")
public class PatientProfile {

	@Id
	@Column(name = "user_id")
	private UUID userId;

	@OneToOne(optional = false)
	@MapsId
	@JoinColumn(name = "user_id")
	private User user;

	@Column(name = "full_name", nullable = false)
	private String fullName;

	@Column(nullable = false, unique = true, length = 11)
	private String cpf;

	@Column(name = "birth_date", nullable = false)
	private LocalDate birthDate;

	private String phone;

	protected PatientProfile() {
	}

	public static PatientProfile of(User user, String fullName, String cpf, LocalDate birthDate, String phone) {
		PatientProfile profile = new PatientProfile();
		profile.user = user;
		profile.fullName = fullName;
		profile.cpf = cpf;
		profile.birthDate = birthDate;
		profile.phone = phone;
		return profile;
	}

	public UUID getUserId() {
		return userId;
	}

	public String getFullName() {
		return fullName;
	}

	public String getCpf() {
		return cpf;
	}

	public LocalDate getBirthDate() {
		return birthDate;
	}

	public String getPhone() {
		return phone;
	}
}
