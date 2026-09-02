package br.com.vidaconecta.scheduling.application;

import br.com.vidaconecta.identity.api.CurrentUser;
import br.com.vidaconecta.identity.api.IdentityFacade;
import br.com.vidaconecta.scheduling.api.AppointmentStatus;
import br.com.vidaconecta.scheduling.domain.Appointment;
import br.com.vidaconecta.scheduling.domain.DoctorAvailability;
import br.com.vidaconecta.scheduling.infrastructure.AppointmentRepository;
import br.com.vidaconecta.scheduling.infrastructure.DoctorAvailabilityRepository;
import br.com.vidaconecta.scheduling.web.AvailabilityResponse;
import br.com.vidaconecta.scheduling.web.AvailableSlotResponse;
import br.com.vidaconecta.scheduling.web.CreateAvailabilityRequest;
import br.com.vidaconecta.shared.api.BusinessException;
import br.com.vidaconecta.shared.api.ConflictException;
import br.com.vidaconecta.shared.api.ForbiddenException;
import br.com.vidaconecta.shared.api.NotFoundException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AvailabilityService {

	static final ZoneId CLINIC_ZONE = ZoneId.of("America/Sao_Paulo");
	private static final int DEFAULT_SLOT_MINUTES = 30;
	private static final int DAYS_AHEAD = 14;

	private final DoctorAvailabilityRepository availabilityRepository;
	private final AppointmentRepository appointmentRepository;
	private final IdentityFacade identityFacade;

	public AvailabilityService(
			DoctorAvailabilityRepository availabilityRepository,
			AppointmentRepository appointmentRepository,
			IdentityFacade identityFacade) {
		this.availabilityRepository = availabilityRepository;
		this.appointmentRepository = appointmentRepository;
		this.identityFacade = identityFacade;
	}

	@Transactional(readOnly = true)
	public List<AvailabilityResponse> listMine(CurrentUser currentUser) {
		requireDoctor(currentUser);
		return availabilityRepository.findByDoctorIdOrderByDayOfWeekAscStartTimeAsc(currentUser.id()).stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<AvailabilityResponse> listByDoctor(UUID doctorId) {
		if (!identityFacade.isActiveDoctor(doctorId)) {
			throw new NotFoundException("Médico não encontrado");
		}
		return availabilityRepository.findByDoctorIdOrderByDayOfWeekAscStartTimeAsc(doctorId).stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional
	public AvailabilityResponse create(CurrentUser currentUser, CreateAvailabilityRequest request) {
		requireDoctor(currentUser);
		LocalTime start = request.startTime();
		LocalTime end = request.endTime();
		if (!start.isBefore(end) && !end.equals(LocalTime.of(23, 59))) {
			throw new BusinessException("O horário final deve ser depois do inicial");
		}
		int slotMinutes = request.slotMinutes() == null ? DEFAULT_SLOT_MINUTES : request.slotMinutes();
		DoctorAvailability candidate = DoctorAvailability.of(
				currentUser.id(),
				request.dayOfWeek(),
				start,
				end,
				slotMinutes);
		boolean overlap = availabilityRepository.findByDoctorIdOrderByDayOfWeekAscStartTimeAsc(currentUser.id())
				.stream()
				.anyMatch(existing -> existing.overlaps(candidate));
		if (overlap) {
			throw new ConflictException("Já existe um horário cadastrado que cruza este intervalo");
		}
		availabilityRepository.save(candidate);
		return toResponse(candidate);
	}

	@Transactional
	public void delete(CurrentUser currentUser, UUID availabilityId) {
		requireDoctor(currentUser);
		DoctorAvailability availability = availabilityRepository.findById(availabilityId)
				.orElseThrow(() -> new NotFoundException("Horário não encontrado"));
		if (!availability.getDoctorId().equals(currentUser.id())) {
			throw new ForbiddenException("Você só pode excluir os próprios horários");
		}
		availabilityRepository.delete(availability);
	}

	@Transactional(readOnly = true)
	public List<AvailableSlotResponse> listSlots(UUID doctorId, Instant now) {
		if (!identityFacade.isActiveDoctor(doctorId)) {
			throw new NotFoundException("Médico não encontrado");
		}
		List<DoctorAvailability> windows = availabilityRepository.findByDoctorIdOrderByDayOfWeekAscStartTimeAsc(doctorId);
		if (windows.isEmpty()) {
			return List.of();
		}
		Instant horizon = now.plus(Duration.ofDays(DAYS_AHEAD));
		List<Appointment> booked = appointmentRepository.findDoctorAppointmentsInWindow(
				doctorId,
				now.minus(Duration.ofHours(2)),
				horizon.plus(Duration.ofHours(2)),
				AppointmentStatus.CANCELLED);
		List<AvailableSlotResponse> slots = new ArrayList<>();
		LocalDate today = now.atZone(CLINIC_ZONE).toLocalDate();
		for (int dayOffset = 0; dayOffset < DAYS_AHEAD; dayOffset++) {
			LocalDate date = today.plusDays(dayOffset);
			for (DoctorAvailability window : windows) {
				if (window.getDayOfWeek() != date.getDayOfWeek()) {
					continue;
				}
				slots.addAll(slotsFor(window, date, now, booked));
			}
		}
		return slots;
	}

	@Transactional(readOnly = true)
	public void assertBookable(UUID doctorId, Instant scheduledAt, int durationMinutes) {
		List<DoctorAvailability> windows = availabilityRepository.findByDoctorIdOrderByDayOfWeekAscStartTimeAsc(doctorId);
		if (windows.isEmpty()) {
			throw new BusinessException("Este médico ainda não cadastrou horários disponíveis");
		}
		ZonedDateTime local = scheduledAt.atZone(CLINIC_ZONE);
		boolean inside = windows.stream()
				.filter(window -> window.getDayOfWeek() == local.getDayOfWeek())
				.anyMatch(window -> window.covers(local.toLocalTime(), durationMinutes));
		if (!inside) {
			throw new BusinessException("Horário fora da agenda disponível deste médico");
		}
	}

	private List<AvailableSlotResponse> slotsFor(
			DoctorAvailability window,
			LocalDate date,
			Instant now,
			List<Appointment> booked) {
		List<AvailableSlotResponse> slots = new ArrayList<>();
		int duration = window.getSlotMinutes();
		int startMinute = window.getStartTime().getHour() * 60 + window.getStartTime().getMinute();
		int endMinute = window.effectiveEnd().equals(LocalTime.MAX)
				? 24 * 60
				: window.effectiveEnd().getHour() * 60 + window.effectiveEnd().getMinute();
		for (int minute = startMinute; minute + duration <= endMinute; minute += duration) {
			LocalTime cursor = LocalTime.of(minute / 60, minute % 60);
			Instant startInstant = ZonedDateTime.of(date, cursor, CLINIC_ZONE).toInstant();
			Instant endInstant = startInstant.plus(Duration.ofMinutes(duration));
			if (!startInstant.isBefore(now) && booked.stream().noneMatch(existing -> existing.overlaps(startInstant, endInstant))) {
				slots.add(new AvailableSlotResponse(startInstant, duration));
			}
		}
		return slots;
	}

	private void requireDoctor(CurrentUser currentUser) {
		if (!currentUser.isDoctor()) {
			throw new ForbiddenException("Somente médicos cadastram horários de atendimento");
		}
	}

	private AvailabilityResponse toResponse(DoctorAvailability availability) {
		return new AvailabilityResponse(
				availability.getId(),
				availability.getDoctorId(),
				availability.getDayOfWeek(),
				availability.getStartTime(),
				availability.getEndTime(),
				availability.getSlotMinutes());
	}
}
