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
@Table(name = "admin_profiles")
public class AdminProfile {

	@Id
	@Column(name = "user_id")
	private UUID userId;

	@OneToOne(optional = false)
	@MapsId
	@JoinColumn(name = "user_id")
	private User user;

	@Column(name = "full_name", nullable = false)
	private String fullName;

	protected AdminProfile() {
	}

	public static AdminProfile of(User user, String fullName) {
		AdminProfile profile = new AdminProfile();
		profile.user = user;
		profile.fullName = fullName;
		return profile;
	}

	public UUID getUserId() {
		return userId;
	}

	public String getFullName() {
		return fullName;
	}
}
