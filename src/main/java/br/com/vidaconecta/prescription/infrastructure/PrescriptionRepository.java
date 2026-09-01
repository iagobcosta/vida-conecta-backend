package br.com.vidaconecta.prescription.infrastructure;

import br.com.vidaconecta.prescription.domain.Prescription;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrescriptionRepository extends JpaRepository<Prescription, UUID> {

	List<Prescription> findByPatientIdOrderByIssuedAtDesc(UUID patientId);

	List<Prescription> findByDoctorIdOrderByIssuedAtDesc(UUID doctorId);
}
