package br.com.vidaconecta.scheduling.infrastructure;

import br.com.vidaconecta.scheduling.domain.DoctorAvailability;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorAvailabilityRepository extends JpaRepository<DoctorAvailability, UUID> {

	List<DoctorAvailability> findByDoctorIdOrderByDayOfWeekAscStartTimeAsc(UUID doctorId);
}
