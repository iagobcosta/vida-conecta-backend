package br.com.vidaconecta.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "doctor_profiles")
public class DoctorProfile {

	@Id
	@Column(name = "user_id")
	private UUID userId;

	@OneToOne(optional = false)
	@MapsId
	@JoinColumn(name = "user_id")
	private User user;

	@Column(name = "full_name", nullable = false)
	private String fullName;

	@Column(nullable = false, unique = true)
	private String crm;

	@Column(nullable = false)
	private String specialty;

	protected DoctorProfile() {
	}

	public static DoctorProfile of(User user, String fullName, String crm, String specialty) {
		DoctorProfile profile = new DoctorProfile();
		profile.user = user;
		profile.fullName = fullName;
		profile.crm = crm;
		profile.specialty = specialty;
		return profile;
	}

	public UUID getUserId() {
		return userId;
	}

	public User getUser() {
		return user;
	}

	public String getFullName() {
		return fullName;
	}

	public String getCrm() {
		return crm;
	}

	public String getSpecialty() {
		return specialty;
	}
}
