package br.com.vidaconecta.consent.infrastructure;

import br.com.vidaconecta.consent.domain.Consent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConsentRepository extends JpaRepository<Consent, UUID> {

	List<Consent> findByPatientIdOrderByGrantedAtDesc(UUID patientId);

	List<Consent> findByDoctorIdOrderByGrantedAtDesc(UUID doctorId);

	@Query("select coalesce(max(c.version), 0) from Consent c where c.patientId = :patientId and c.doctorId = :doctorId")
	int nextVersionBase(@Param("patientId") UUID patientId, @Param("doctorId") UUID doctorId);

	List<Consent> findByPatientIdAndDoctorIdAndRevokedAtIsNull(UUID patientId, UUID doctorId);
}
