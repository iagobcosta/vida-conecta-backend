package br.com.vidaconecta.identity.infrastructure;

import br.com.vidaconecta.identity.domain.DoctorInvite;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorInviteRepository extends JpaRepository<DoctorInvite, UUID> {

	Optional<DoctorInvite> findByToken(UUID token);

	Optional<DoctorInvite> findByEmailIgnoreCaseAndAcceptedAtIsNull(String email);

	List<DoctorInvite> findAllByOrderByCreatedAtDesc();
}
