package br.com.vidaconecta.identity.infrastructure;

import br.com.vidaconecta.identity.api.Role;
import br.com.vidaconecta.identity.domain.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

	Optional<User> findByEmailIgnoreCase(String email);

	boolean existsByEmailIgnoreCase(String email);

	long countByRole(Role role);

	long countByRoleAndEnabled(Role role, boolean enabled);
}
