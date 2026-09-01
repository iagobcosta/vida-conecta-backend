package br.com.vidaconecta.identity.infrastructure;

import br.com.vidaconecta.identity.domain.DoctorProfile;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DoctorProfileRepository extends JpaRepository<DoctorProfile, UUID> {

	boolean existsByCrm(String crm);

	Optional<DoctorProfile> findByUserId(UUID userId);

	List<DoctorProfile> findAllByOrderByFullNameAsc();

	@Query("select p from DoctorProfile p join fetch p.user order by p.fullName asc")
	List<DoctorProfile> findAllWithUserOrderByFullNameAsc();
}
