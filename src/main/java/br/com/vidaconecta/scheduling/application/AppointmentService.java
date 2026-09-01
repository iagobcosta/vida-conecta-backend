package br.com.vidaconecta.scheduling.application;

import br.com.vidaconecta.identity.api.CurrentUser;
import br.com.vidaconecta.identity.api.IdentityFacade;
import br.com.vidaconecta.identity.api.Role;
import br.com.vidaconecta.notification.api.NotificationFacade;
import br.com.vidaconecta.notification.api.NotificationType;
import br.com.vidaconecta.scheduling.api.AppointmentStatus;
import br.com.vidaconecta.scheduling.domain.Appointment;
import br.com.vidaconecta.scheduling.infrastructure.AppointmentRepository;
import br.com.vidaconecta.scheduling.web.AppointmentResponse;
import br.com.vidaconecta.scheduling.web.CancelAppointmentRequest;
import br.com.vidaconecta.scheduling.web.CreateAppointmentRequest;
import br.com.vidaconecta.scheduling.web.DoctorResponse;
import br.com.vidaconecta.shared.api.BusinessException;
import br.com.vidaconecta.shared.api.ConflictException;
import br.com.vidaconecta.shared.api.ForbiddenException;
import br.com.vidaconecta.shared.api.NotFoundException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppointmentService {

	private static final int DEFAULT_DURATION_MINUTES = 30;
	private static final int MIN_DURATION = 15;
	private static final int MAX_DURATION = 120;
	private static final int MIN_DOCTOR_CANCEL_REASON = 10;
	private static final ZoneId CLINIC_ZONE = ZoneId.of("America/Sao_Paulo");
	private static final DateTimeFormatter CLINIC_WHEN = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm")
			.withZone(CLINIC_ZONE);

	private final AppointmentRepository appointmentRepository;
	private final IdentityFacade identityFacade;
	private final AvailabilityService availabilityService;
	private final NotificationFacade notificationFacade;
	private final int joinWindowMinutesBefore;

	public AppointmentService(
			AppointmentRepository appointmentRepository,
			IdentityFacade identityFacade,
			AvailabilityService availabilityService,
			NotificationFacade notificationFacade,
			@Value("${vida-conecta.video.join-window-minutes-before:10}") int joinWindowMinutesBefore) {
		this.appointmentRepository = appointmentRepository;
		this.identityFacade = identityFacade;
		this.availabilityService = availabilityService;
		this.notificationFacade = notificationFacade;
		this.joinWindowMinutesBefore = joinWindowMinutesBefore;
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
		availabilityService.assertBookable(request.doctorId(), scheduledAt, duration);
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
		String patientName = identityFacade.displayName(currentUser.id());
		notificationFacade.push(new NotificationFacade.NewNotification(
				request.doctorId(),
				NotificationType.APPOINTMENT_SCHEDULED,
				"Nova consulta agendada",
				patientName + " agendou uma consulta para " + formatWhen(scheduledAt) + ". Confirme para liberar a sala.",
				appointment.getId(),
				"/agenda",
				"Confirmar consulta"));
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
		String doctorName = identityFacade.displayName(currentUser.id());
		notificationFacade.push(new NotificationFacade.NewNotification(
				appointment.getPatientId(),
				NotificationType.APPOINTMENT_CONFIRMED,
				"Consulta confirmada",
				doctorName + " confirmou sua consulta em " + formatWhen(appointment.getScheduledAt()) + ".",
				appointment.getId(),
				"/consulta/" + appointment.getId(),
				"Abrir consulta"));
		return toResponse(appointment);
	}

	@Transactional
	public AppointmentResponse cancel(CurrentUser currentUser, UUID appointmentId, CancelAppointmentRequest request) {
		Appointment appointment = requireParticipant(currentUser, appointmentId);
		String reason = request == null ? null : trimToNull(request.reason());
		if (currentUser.isDoctor()) {
			if (reason == null || reason.length() < MIN_DOCTOR_CANCEL_REASON) {
				throw new BusinessException(
						"Informe o motivo do cancelamento (mínimo 10 caracteres) para o paciente poder reagendar");
			}
		}
		try {
			appointment.cancel(currentUser.id(), reason);
		} catch (IllegalStateException exception) {
			throw new BusinessException(exception.getMessage());
		}
		notifyCancellation(currentUser, appointment, reason);
		return toResponse(appointment);
	}

	@Transactional
	public AppointmentResponse complete(CurrentUser currentUser, UUID appointmentId) {
		Appointment appointment = requireOwnedByDoctor(currentUser, appointmentId);
		try {
			appointment.complete();
		} catch (IllegalStateException exception) {
			throw new BusinessException(exception.getMessage());
		}
		notificationFacade.push(new NotificationFacade.NewNotification(
				appointment.getPatientId(),
				NotificationType.APPOINTMENT_COMPLETED,
				"Consulta concluída",
				"Sua consulta com " + identityFacade.displayName(appointment.getDoctorId())
						+ " foi concluída. Receitas e evolução ficam disponíveis no histórico.",
				appointment.getId(),
				"/prontuario",
				"Ver prontuário"));
		return toResponse(appointment);
	}

	private void notifyCancellation(CurrentUser actor, Appointment appointment, String reason) {
		boolean doctorCancelled = actor.isDoctor() || appointment.getDoctorId().equals(actor.id());
		UUID recipientId = doctorCancelled ? appointment.getPatientId() : appointment.getDoctorId();
		String actorName = identityFacade.displayName(actor.id());
		String when = formatWhen(appointment.getScheduledAt());
		String body;
		String actionPath;
		String actionLabel;
		if (doctorCancelled) {
			body = actorName + " cancelou a consulta de " + when + ". Motivo: " + reason;
			actionPath = "/agenda/nova?medico=" + appointment.getDoctorId();
			actionLabel = "Reagendar consulta";
		} else {
			body = actorName + " cancelou a consulta de " + when
					+ (reason == null ? "." : ". Motivo: " + reason);
			actionPath = "/agenda";
			actionLabel = "Ver agenda";
		}
		notificationFacade.push(new NotificationFacade.NewNotification(
				recipientId,
				NotificationType.APPOINTMENT_CANCELLED,
				"Consulta cancelada",
				body,
				appointment.getId(),
				actionPath,
				actionLabel));
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
			throw new ForbiddenException("Somente o médico da consulta pode alterar o status");
		}
		Appointment appointment = requireParticipant(currentUser, appointmentId);
		if (!appointment.getDoctorId().equals(currentUser.id())) {
			throw new ForbiddenException("Somente o médico da consulta pode alterar o status");
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
		Instant joinOpensAt = appointment.getScheduledAt().minus(Duration.ofMinutes(joinWindowMinutesBefore));
		Instant joinClosesAt = appointment.endsAt();
		Instant now = Instant.now();
		boolean canJoinNow = appointment.isJoinable()
				&& !now.isBefore(joinOpensAt)
				&& !now.isAfter(joinClosesAt);
		String cancelledByName = appointment.getCancelledBy() == null
				? null
				: identityFacade.displayName(appointment.getCancelledBy());
		return new AppointmentResponse(
				appointment.getId(),
				appointment.getPatientId(),
				patientName,
				appointment.getDoctorId(),
				doctorName,
				appointment.getScheduledAt(),
				appointment.getDurationMinutes(),
				appointment.getStatus(),
				joinOpensAt,
				joinClosesAt,
				canJoinNow,
				appointment.getCancelReason(),
				appointment.getCancelledBy(),
				cancelledByName);
	}

	private static String formatWhen(Instant instant) {
		return CLINIC_WHEN.format(instant);
	}

	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
