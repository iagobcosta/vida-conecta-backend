package br.com.vidaconecta.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "doctor_invites")
public class DoctorInvite {

	@Id
	private UUID id;

	@Column(nullable = false)
	private String email;

	@Column(name = "full_name", nullable = false)
	private String fullName;

	@Column(nullable = false, unique = true)
	private UUID token;

	@Column(name = "invited_by", nullable = false)
	private UUID invitedBy;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "accepted_at")
	private Instant acceptedAt;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected DoctorInvite() {
	}

	public static DoctorInvite create(String email, String fullName, UUID invitedBy, Instant expiresAt) {
		DoctorInvite invite = new DoctorInvite();
		invite.id = UUID.randomUUID();
		invite.email = email;
		invite.fullName = fullName;
		invite.token = UUID.randomUUID();
		invite.invitedBy = invitedBy;
		invite.expiresAt = expiresAt;
		invite.createdAt = Instant.now();
		return invite;
	}

	public UUID getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getFullName() {
		return fullName;
	}

	public UUID getToken() {
		return token;
	}

	public UUID getInvitedBy() {
		return invitedBy;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public Instant getAcceptedAt() {
		return acceptedAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public boolean isAccepted() {
		return acceptedAt != null;
	}

	public boolean isExpired(Instant now) {
		return now.isAfter(expiresAt);
	}

	public void refresh(String fullName, Instant expiresAt) {
		this.fullName = fullName;
		this.token = UUID.randomUUID();
		this.expiresAt = expiresAt;
	}

	public void accept() {
		if (acceptedAt != null) {
			throw new IllegalStateException("Este convite já foi utilizado");
		}
		acceptedAt = Instant.now();
	}
}
