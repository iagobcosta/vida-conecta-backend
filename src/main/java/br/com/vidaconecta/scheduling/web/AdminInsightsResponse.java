package br.com.vidaconecta.scheduling.web;

import java.time.Instant;
import java.util.List;

public record AdminInsightsResponse(
		Instant generatedAt,
		String clinicTimeZone,
		CensusTotals census,
		AppointmentTotals appointments,
		List<DailyAppointmentPoint> last30Days,
		List<SpecialtyShare> bySpecialty) {

	public record CensusTotals(
			long patients,
			long doctorsActive,
			long doctorsInactive,
			long admins,
			long pendingInvites) {
	}

	public record AppointmentTotals(
			long total,
			long scheduled,
			long confirmed,
			long cancelled,
			long inProgress,
			long completed,
			long today,
			long upcoming,
			long last7Days,
			long previous7Days,
			double cancellationRate) {
	}

	public record DailyAppointmentPoint(
			String date,
			long created,
			long scheduled,
			long confirmed,
			long cancelled,
			long inProgress,
			long completed) {
	}

	public record SpecialtyShare(String specialty, long doctors, long appointments) {
	}
}
