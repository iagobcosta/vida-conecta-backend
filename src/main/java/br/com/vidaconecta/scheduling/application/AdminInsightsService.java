package br.com.vidaconecta.scheduling.application;

import br.com.vidaconecta.identity.api.IdentityFacade;
import br.com.vidaconecta.identity.api.IdentityFacade.DoctorView;
import br.com.vidaconecta.identity.api.IdentityFacade.SystemCensus;
import br.com.vidaconecta.scheduling.api.AppointmentStatus;
import br.com.vidaconecta.scheduling.domain.Appointment;
import br.com.vidaconecta.scheduling.infrastructure.AppointmentRepository;
import br.com.vidaconecta.scheduling.web.AdminInsightsResponse;
import br.com.vidaconecta.scheduling.web.AdminInsightsResponse.AppointmentTotals;
import br.com.vidaconecta.scheduling.web.AdminInsightsResponse.CensusTotals;
import br.com.vidaconecta.scheduling.web.AdminInsightsResponse.DailyAppointmentPoint;
import br.com.vidaconecta.scheduling.web.AdminInsightsResponse.SpecialtyShare;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminInsightsService {

	static final ZoneId CLINIC_ZONE = ZoneId.of("America/Sao_Paulo");
	private static final int SERIES_DAYS = 30;

	private final AppointmentRepository appointmentRepository;
	private final IdentityFacade identityFacade;

	public AdminInsightsService(AppointmentRepository appointmentRepository, IdentityFacade identityFacade) {
		this.appointmentRepository = appointmentRepository;
		this.identityFacade = identityFacade;
	}

	public AdminInsightsResponse insights() {
		Instant now = Instant.now();
		LocalDate today = LocalDate.now(CLINIC_ZONE);
		List<Appointment> appointments = appointmentRepository.findAll();
		Map<AppointmentStatus, Long> byStatus = countByStatus(appointments);

		long total = appointments.size();
		long cancelled = byStatus.getOrDefault(AppointmentStatus.CANCELLED, 0L);
		Instant startOfToday = today.atStartOfDay(CLINIC_ZONE).toInstant();
		Instant startOfTomorrow = today.plusDays(1).atStartOfDay(CLINIC_ZONE).toInstant();
		Instant last7Start = today.minusDays(6).atStartOfDay(CLINIC_ZONE).toInstant();
		Instant previous7Start = today.minusDays(13).atStartOfDay(CLINIC_ZONE).toInstant();

		long todayCount = countInWindow(appointments, startOfToday, startOfTomorrow);
		long last7Days = countInWindow(appointments, last7Start, startOfTomorrow);
		long previous7Days = countInWindow(appointments, previous7Start, last7Start);
		long upcoming = appointments.stream()
				.filter(appointment -> !appointment.getScheduledAt().isBefore(now))
				.filter(appointment -> appointment.getStatus() != AppointmentStatus.CANCELLED
						&& appointment.getStatus() != AppointmentStatus.COMPLETED)
				.count();
		double cancellationRate = total == 0 ? 0 : roundRate(cancelled / (double) total);

		SystemCensus census = identityFacade.census();
		return new AdminInsightsResponse(
				now,
				CLINIC_ZONE.getId(),
				new CensusTotals(
						census.patients(),
						census.doctorsActive(),
						census.doctorsInactive(),
						census.admins(),
						census.pendingInvites()),
				new AppointmentTotals(
						total,
						byStatus.getOrDefault(AppointmentStatus.SCHEDULED, 0L),
						byStatus.getOrDefault(AppointmentStatus.CONFIRMED, 0L),
						cancelled,
						byStatus.getOrDefault(AppointmentStatus.IN_PROGRESS, 0L),
						byStatus.getOrDefault(AppointmentStatus.COMPLETED, 0L),
						todayCount,
						upcoming,
						last7Days,
						previous7Days,
						cancellationRate),
				dailySeries(appointments, today),
				specialtyShare(appointments));
	}

	private List<DailyAppointmentPoint> dailySeries(List<Appointment> appointments, LocalDate today) {
		LocalDate from = today.minusDays(SERIES_DAYS - 1L);
		Map<LocalDate, DailyBucket> buckets = new LinkedHashMap<>();
		for (LocalDate day = from; !day.isAfter(today); day = day.plusDays(1)) {
			buckets.put(day, new DailyBucket());
		}
		for (Appointment appointment : appointments) {
			LocalDate scheduledDay = appointment.getScheduledAt().atZone(CLINIC_ZONE).toLocalDate();
			DailyBucket scheduledBucket = buckets.get(scheduledDay);
			if (scheduledBucket != null) {
				scheduledBucket.addStatus(appointment.getStatus());
			}
			LocalDate createdDay = appointment.getCreatedAt().atZone(CLINIC_ZONE).toLocalDate();
			DailyBucket createdBucket = buckets.get(createdDay);
			if (createdBucket != null) {
				createdBucket.created += 1;
			}
		}
		List<DailyAppointmentPoint> points = new ArrayList<>();
		buckets.forEach((date, bucket) -> points.add(bucket.toPoint(date)));
		return points;
	}

	private List<SpecialtyShare> specialtyShare(List<Appointment> appointments) {
		List<DoctorView> doctors = identityFacade.listAllDoctors();
		Map<String, long[]> bySpecialty = new LinkedHashMap<>();
		for (DoctorView doctor : doctors) {
			bySpecialty.computeIfAbsent(doctor.specialty(), key -> new long[2])[0] += 1;
		}
		Map<UUID, String> specialtyByDoctor = new LinkedHashMap<>();
		for (DoctorView doctor : doctors) {
			specialtyByDoctor.put(doctor.userId(), doctor.specialty());
		}
		for (Appointment appointment : appointments) {
			String specialty = specialtyByDoctor.getOrDefault(appointment.getDoctorId(), "Outros");
			bySpecialty.computeIfAbsent(specialty, key -> new long[2])[1] += 1;
		}
		return bySpecialty.entrySet().stream()
				.map(entry -> new SpecialtyShare(entry.getKey(), entry.getValue()[0], entry.getValue()[1]))
				.sorted((left, right) -> Long.compare(right.appointments(), left.appointments()))
				.toList();
	}

	private Map<AppointmentStatus, Long> countByStatus(List<Appointment> appointments) {
		Map<AppointmentStatus, Long> counts = new EnumMap<>(AppointmentStatus.class);
		for (Appointment appointment : appointments) {
			counts.merge(appointment.getStatus(), 1L, Long::sum);
		}
		return counts;
	}

	private long countInWindow(List<Appointment> appointments, Instant from, Instant to) {
		return appointments.stream()
				.filter(appointment -> !appointment.getScheduledAt().isBefore(from) && appointment.getScheduledAt().isBefore(to))
				.count();
	}

	private double roundRate(double value) {
		return Math.round(value * 1000d) / 1000d;
	}

	private static final class DailyBucket {
		private long created;
		private long scheduled;
		private long confirmed;
		private long cancelled;
		private long inProgress;
		private long completed;

		private void addStatus(AppointmentStatus status) {
			switch (status) {
				case SCHEDULED -> scheduled += 1;
				case CONFIRMED -> confirmed += 1;
				case CANCELLED -> cancelled += 1;
				case IN_PROGRESS -> inProgress += 1;
				case COMPLETED -> completed += 1;
			}
		}

		private DailyAppointmentPoint toPoint(LocalDate date) {
			return new DailyAppointmentPoint(date.toString(), created, scheduled, confirmed, cancelled, inProgress, completed);
		}
	}
}
