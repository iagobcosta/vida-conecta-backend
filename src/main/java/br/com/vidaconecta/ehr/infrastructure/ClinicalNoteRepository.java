package br.com.vidaconecta.ehr.infrastructure;

import br.com.vidaconecta.ehr.domain.ClinicalNote;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClinicalNoteRepository extends JpaRepository<ClinicalNote, UUID> {

	List<ClinicalNote> findByPatientIdOrderByCreatedAtDesc(UUID patientId);
}
