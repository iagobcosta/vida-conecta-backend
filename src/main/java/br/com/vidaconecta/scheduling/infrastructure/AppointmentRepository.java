package br.com.vidaconecta.scheduling.infrastructure;

import br.com.vidaconecta.scheduling.api.AppointmentStatus;
import br.com.vidaconecta.scheduling.domain.Appointment;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

	List<Appointment> findByPatientIdOrderByScheduledAtDesc(UUID patientId);

	List<Appointment> findByDoctorIdOrderByScheduledAtDesc(UUID doctorId);

	boolean existsByPatientIdAndDoctorIdAndStatusNot(UUID patientId, UUID doctorId, AppointmentStatus status);

	@Query("""
			select a from Appointment a
			where a.doctorId = :doctorId
			  and a.status <> :cancelled
			  and a.scheduledAt >= :from
			  and a.scheduledAt < :to
			""")
	List<Appointment> findDoctorAppointmentsInWindow(
			@Param("doctorId") UUID doctorId,
			@Param("from") Instant from,
			@Param("to") Instant to,
			@Param("cancelled") AppointmentStatus cancelled);
}
