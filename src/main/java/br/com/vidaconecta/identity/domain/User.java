package br.com.vidaconecta.identity.domain;

import br.com.vidaconecta.identity.api.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

	@Id
	private UUID id;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Role role;

	@Column(nullable = false)
	private boolean enabled;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@OneToOne(mappedBy = "user")
	private PatientProfile patientProfile;

	@OneToOne(mappedBy = "user")
	private DoctorProfile doctorProfile;

	@OneToOne(mappedBy = "user")
	private AdminProfile adminProfile;

	protected User() {
	}

	public static User create(String email, String passwordHash, Role role) {
		User user = new User();
		user.id = UUID.randomUUID();
		user.email = email;
		user.passwordHash = passwordHash;
		user.role = role;
		user.enabled = true;
		Instant now = Instant.now();
		user.createdAt = now;
		user.updatedAt = now;
		return user;
	}

	public UUID getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public Role getRole() {
		return role;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
		this.updatedAt = Instant.now();
	}

	public PatientProfile getPatientProfile() {
		return patientProfile;
	}

	public DoctorProfile getDoctorProfile() {
		return doctorProfile;
	}

	public AdminProfile getAdminProfile() {
		return adminProfile;
	}

	public void attachPatientProfile(PatientProfile profile) {
		this.patientProfile = profile;
	}

	public void attachDoctorProfile(DoctorProfile profile) {
		this.doctorProfile = profile;
	}

	public void attachAdminProfile(AdminProfile profile) {
		this.adminProfile = profile;
	}
}
