package br.com.vidaconecta.ehr.infrastructure;

import br.com.vidaconecta.ehr.domain.ClinicalNote;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ClinicalNoteRepository extends JpaRepository<ClinicalNote, UUID> {

	List<ClinicalNote> findByPatientIdOrderByCreatedAtDesc(UUID patientId);

	@Modifying
    @Query("DELETE FROM ClinicalNote c WHERE c.createdAt < :cutoff")
    int deleteByCreatedAtBefore(Instant cutoff);
}
