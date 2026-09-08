package br.com.vidaconecta.ehr.infrastructure;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import br.com.vidaconecta.ehr.domain.EhrAccessAudit;

public interface EhrAccessAuditRepository extends JpaRepository<EhrAccessAudit, UUID> {

	List<EhrAccessAudit> findByPatientIdOrderByAccessedAtDesc(UUID patientId);

	@Modifying
    @Query("DELETE FROM EhrAccessAudit a WHERE a.accessedAt < :cutoff")
    int deleteByAccessedAtBefore(Instant cutoff);
}
