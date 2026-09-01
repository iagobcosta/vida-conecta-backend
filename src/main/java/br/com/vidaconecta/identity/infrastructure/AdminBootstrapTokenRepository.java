package br.com.vidaconecta.identity.infrastructure;

import br.com.vidaconecta.identity.domain.AdminBootstrapToken;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface AdminBootstrapTokenRepository extends JpaRepository<AdminBootstrapToken, UUID> {

	Optional<AdminBootstrapToken> findFirstByOrderByCreatedAtAsc();

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select t from AdminBootstrapToken t order by t.createdAt asc")
	Optional<AdminBootstrapToken> lockCurrent();
}
