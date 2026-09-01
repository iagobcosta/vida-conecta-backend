package br.com.vidaconecta.scheduling.application;

import br.com.vidaconecta.scheduling.api.AppointmentStatus;
import br.com.vidaconecta.scheduling.api.SchedulingFacade;
import br.com.vidaconecta.scheduling.domain.Appointment;
import br.com.vidaconecta.scheduling.infrastructure.AppointmentRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SchedulingFacadeImpl implements SchedulingFacade {

	private final AppointmentRepository appointmentRepository;
	private final int joinWindowMinutesBefore;

	public SchedulingFacadeImpl(
			AppointmentRepository appointmentRepository,
			@Value("${vida-conecta.video.join-window-minutes-before:10}") int joinWindowMinutesBefore) {
		this.appointmentRepository = appointmentRepository;
		this.joinWindowMinutesBefore = joinWindowMinutesBefore;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<AppointmentView> findById(UUID appointmentId) {
		return appointmentRepository.findById(appointmentId).map(this::toView);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean isParticipant(UUID appointmentId, UUID userId) {
		return appointmentRepository.findById(appointmentId)
				.filter(appointment -> appointment.isParticipant(userId))
				.isPresent();
	}

	@Override
	@Transactional(readOnly = true)
	public boolean hasRelationship(UUID patientId, UUID doctorId) {
		return appointmentRepository.existsByPatientIdAndDoctorIdAndStatusNot(
				patientId, doctorId, AppointmentStatus.CANCELLED);
	}

	@Override
	public boolean canJoinVideo(UUID appointmentId, UUID userId, Instant now) {
		Appointment appointment = appointmentRepository.findById(appointmentId).orElse(null);
		if (appointment == null || !appointment.isParticipant(userId) || !appointment.isJoinable()) {
			return false;
		}
		Instant windowStart = appointment.getScheduledAt().minus(Duration.ofMinutes(joinWindowMinutesBefore));
		if (now.isBefore(windowStart) || now.isAfter(appointment.endsAt())) {
			return false;
		}
		appointment.markInProgress();
		return true;
	}

	private AppointmentView toView(Appointment appointment) {
		return new AppointmentView(
				appointment.getId(),
				appointment.getPatientId(),
				appointment.getDoctorId(),
				appointment.getScheduledAt(),
				appointment.getDurationMinutes(),
				appointment.getStatus());
	}
}
