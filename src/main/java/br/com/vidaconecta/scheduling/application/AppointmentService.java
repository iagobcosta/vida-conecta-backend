package br.com.vidaconecta.scheduling.application;

import br.com.vidaconecta.identity.api.CurrentUser;
import br.com.vidaconecta.identity.api.IdentityFacade;
import br.com.vidaconecta.identity.api.Role;
import br.com.vidaconecta.scheduling.api.AppointmentStatus;
import br.com.vidaconecta.scheduling.domain.Appointment;
import br.com.vidaconecta.scheduling.infrastructure.AppointmentRepository;
import br.com.vidaconecta.scheduling.web.AppointmentResponse;
import br.com.vidaconecta.scheduling.web.CreateAppointmentRequest;
import br.com.vidaconecta.scheduling.web.DoctorResponse;
import br.com.vidaconecta.shared.api.BusinessException;
import br.com.vidaconecta.shared.api.ConflictException;
import br.com.vidaconecta.shared.api.ForbiddenException;
import br.com.vidaconecta.shared.api.NotFoundException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppointmentService {

	private static final int DEFAULT_DURATION_MINUTES = 30;
	private static final int MIN_DURATION = 15;
	private static final int MAX_DURATION = 120;

	private final AppointmentRepository appointmentRepository;
	private final IdentityFacade identityFacade;

	public AppointmentService(AppointmentRepository appointmentRepository, IdentityFacade identityFacade) {
		this.appointmentRepository = appointmentRepository;
		this.identityFacade = identityFacade;
	}

	@Transactional(readOnly = true)
	public List<DoctorResponse> listDoctors() {
		return identityFacade.listDoctors().stream()
				.map(doctor -> new DoctorResponse(doctor.userId(), doctor.fullName(), doctor.crm(), doctor.specialty()))
				.toList();
	}

	@Transactional
	public AppointmentResponse create(CurrentUser currentUser, CreateAppointmentRequest request) {
		if (!currentUser.isPatient()) {
			throw new ForbiddenException("Somente pacientes podem agendar consultas");
		}
		if (!identityFacade.isDoctor(request.doctorId())) {
			throw new NotFoundException("Médico não encontrado");
		}
		Instant scheduledAt = request.scheduledAt();
		if (scheduledAt.isBefore(Instant.now())) {
			throw new BusinessException("A consulta deve ser no futuro");
		}
		int duration = request.durationMinutes() == null ? DEFAULT_DURATION_MINUTES : request.durationMinutes();
		if (duration < MIN_DURATION || duration > MAX_DURATION) {
			throw new BusinessException("Duração deve estar entre 15 e 120 minutos");
		}
		Instant end = scheduledAt.plus(Duration.ofMinutes(duration));
		boolean overlap = appointmentRepository
				.findDoctorAppointmentsInWindow(
						request.doctorId(),
						scheduledAt.minus(Duration.ofMinutes(MAX_DURATION)),
						end.plus(Duration.ofMinutes(MAX_DURATION)),
						AppointmentStatus.CANCELLED)
				.stream()
				.anyMatch(existing -> existing.overlaps(scheduledAt, end));
		if (overlap) {
			throw new ConflictException("Horário indisponível para este médico");
		}
		Appointment appointment = Appointment.schedule(currentUser.id(), request.doctorId(), scheduledAt, duration);
		appointmentRepository.save(appointment);
		return toResponse(appointment);
	}

	@Transactional(readOnly = true)
	public List<AppointmentResponse> list(CurrentUser currentUser) {
		List<Appointment> appointments = currentUser.role() == Role.MEDICO
				? appointmentRepository.findByDoctorIdOrderByScheduledAtDesc(currentUser.id())
				: appointmentRepository.findByPatientIdOrderByScheduledAtDesc(currentUser.id());
		return appointments.stream().map(this::toResponse).toList();
	}

	@Transactional(readOnly = true)
	public AppointmentResponse get(CurrentUser currentUser, UUID appointmentId) {
		return toResponse(requireParticipant(currentUser, appointmentId));
	}

	@Transactional
	public AppointmentResponse confirm(CurrentUser currentUser, UUID appointmentId) {
		Appointment appointment = requireOwnedByDoctor(currentUser, appointmentId);
		try {
			appointment.confirm();
		} catch (IllegalStateException exception) {
			throw new BusinessException(exception.getMessage());
		}
		return toResponse(appointment);
	}

	@Transactional
	public AppointmentResponse cancel(CurrentUser currentUser, UUID appointmentId) {
		Appointment appointment = requireParticipant(currentUser, appointmentId);
		try {
			appointment.cancel();
		} catch (IllegalStateException exception) {
			throw new BusinessException(exception.getMessage());
		}
		return toResponse(appointment);
	}

	private Appointment requireParticipant(CurrentUser currentUser, UUID appointmentId) {
		Appointment appointment = appointmentRepository.findById(appointmentId)
				.orElseThrow(() -> new NotFoundException("Consulta não encontrada"));
		if (!appointment.isParticipant(currentUser.id()) && !currentUser.isAdmin()) {
			throw new ForbiddenException("Você não participa desta consulta");
		}
		return appointment;
	}

	private Appointment requireOwnedByDoctor(CurrentUser currentUser, UUID appointmentId) {
		if (!currentUser.isDoctor()) {
			throw new ForbiddenException("Somente o médico pode confirmar a consulta");
		}
		Appointment appointment = requireParticipant(currentUser, appointmentId);
		if (!appointment.getDoctorId().equals(currentUser.id())) {
			throw new ForbiddenException("Somente o médico da consulta pode confirmá-la");
		}
		return appointment;
	}

	private AppointmentResponse toResponse(Appointment appointment) {
		String doctorName = identityFacade.findDoctor(appointment.getDoctorId())
				.map(IdentityFacade.DoctorView::fullName)
				.orElse(null);
		String patientName = identityFacade.findPatient(appointment.getPatientId())
				.map(IdentityFacade.PatientView::fullName)
				.orElse(null);
		return new AppointmentResponse(
				appointment.getId(),
				appointment.getPatientId(),
				patientName,
				appointment.getDoctorId(),
				doctorName,
				appointment.getScheduledAt(),
				appointment.getDurationMinutes(),
				appointment.getStatus());
	}
}
