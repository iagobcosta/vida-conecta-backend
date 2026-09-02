package br.com.vidaconecta.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "admin_bootstrap_tokens")
public class AdminBootstrapToken {

	@Id
	private UUID id;

	@Column(nullable = false, unique = true)
	private UUID token;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected AdminBootstrapToken() {
	}

	public static AdminBootstrapToken of(UUID token) {
		AdminBootstrapToken row = new AdminBootstrapToken();
		row.id = UUID.randomUUID();
		row.token = token;
		row.createdAt = Instant.now();
		return row;
	}

	public UUID getId() {
		return id;
	}

	public UUID getToken() {
		return token;
	}
}
