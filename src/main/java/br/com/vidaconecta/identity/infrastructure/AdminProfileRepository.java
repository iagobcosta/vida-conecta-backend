package br.com.vidaconecta.identity.infrastructure;

import br.com.vidaconecta.identity.domain.AdminProfile;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminProfileRepository extends JpaRepository<AdminProfile, UUID> {

	Optional<AdminProfile> findByUserId(UUID userId);
}
