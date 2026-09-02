package br.com.vidaconecta.identity.infrastructure;

import br.com.vidaconecta.identity.domain.PatientProfile;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientProfileRepository extends JpaRepository<PatientProfile, UUID> {

	boolean existsByCpf(String cpf);

	Optional<PatientProfile> findByUserId(UUID userId);
}
