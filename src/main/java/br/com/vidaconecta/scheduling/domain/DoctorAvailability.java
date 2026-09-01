package br.com.vidaconecta.scheduling.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "doctor_availabilities")
public class DoctorAvailability {

	@Id
	private UUID id;

	@Column(name = "doctor_id", nullable = false)
	private UUID doctorId;

	@Enumerated(EnumType.STRING)
	@Column(name = "day_of_week", nullable = false)
	private DayOfWeek dayOfWeek;

	@Column(name = "start_time", nullable = false)
	private LocalTime startTime;

	@Column(name = "end_time", nullable = false)
	private LocalTime endTime;

	@Column(name = "slot_minutes", nullable = false)
	private int slotMinutes;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected DoctorAvailability() {
	}

	public static DoctorAvailability of(
			UUID doctorId,
			DayOfWeek dayOfWeek,
			LocalTime startTime,
			LocalTime endTime,
			int slotMinutes) {
		DoctorAvailability availability = new DoctorAvailability();
		availability.id = UUID.randomUUID();
		availability.doctorId = doctorId;
		availability.dayOfWeek = dayOfWeek;
		availability.startTime = startTime;
		availability.endTime = endTime;
		availability.slotMinutes = slotMinutes;
		Instant now = Instant.now();
		availability.createdAt = now;
		availability.updatedAt = now;
		return availability;
	}

	public UUID getId() {
		return id;
	}

	public UUID getDoctorId() {
		return doctorId;
	}

	public DayOfWeek getDayOfWeek() {
		return dayOfWeek;
	}

	public LocalTime getStartTime() {
		return startTime;
	}

	public LocalTime getEndTime() {
		return endTime;
	}

	public int getSlotMinutes() {
		return slotMinutes;
	}

	public LocalTime effectiveEnd() {
		if (endTime.equals(LocalTime.of(23, 59))) {
			return LocalTime.MAX;
		}
		return endTime;
	}

	public boolean covers(LocalTime slotStart, int durationMinutes) {
		int start = minutesOfDay(slotStart);
		int end = start + durationMinutes;
		if (end > 24 * 60) {
			return false;
		}
		return start >= minutesOfDay(startTime) && end <= minutesOfDay(effectiveEnd());
	}

	public boolean overlaps(DoctorAvailability other) {
		if (other.dayOfWeek != dayOfWeek) {
			return false;
		}
		return minutesOfDay(startTime) < minutesOfDay(other.effectiveEnd())
				&& minutesOfDay(other.startTime) < minutesOfDay(effectiveEnd());
	}

	private static int minutesOfDay(LocalTime time) {
		if (time.equals(LocalTime.MAX)) {
			return 24 * 60;
		}
		return time.getHour() * 60 + time.getMinute();
	}
}
