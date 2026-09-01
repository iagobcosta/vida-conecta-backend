package br.com.vidaconecta.ehr.infrastructure;

import br.com.vidaconecta.ehr.domain.EhrAccessAudit;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EhrAccessAuditRepository extends JpaRepository<EhrAccessAudit, UUID> {

	List<EhrAccessAudit> findByPatientIdOrderByAccessedAtDesc(UUID patientId);
}
