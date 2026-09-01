package br.com.vidaconecta;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class SchedulingTests extends AbstractIntegrationTest {

	@Test
	void shouldCreateConfirmAndRejectOverlappingAppointment() throws Exception {
		String suffix = uniqueSuffix();
		String patientEmail = "ag.paciente." + suffix + "@vidaconecta.test";
		String doctorEmail = "ag.medico." + suffix + "@vidaconecta.test";
		String patientToken = registerPatient(patientEmail, cpf(suffix, "01"));
		String doctorToken = registerDoctor(doctorEmail, "CRM" + suffix, "Clínica Geral");
		openClinicHours(doctorToken);
		String doctorId = currentUserId(doctorToken).toString();
		Instant start = Instant.now().plus(2, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS);

		MvcResult created = mockMvc.perform(post("/api/v1/appointments")
						.header("Authorization", bearer(patientToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "doctorId": "%s",
								  "scheduledAt": "%s",
								  "durationMinutes": 30
								}
								""".formatted(doctorId, start)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("SCHEDULED"))
				.andExpect(jsonPath("$.canJoinNow").value(false))
				.andReturn();
		String appointmentId = jsonId(created);

		mockMvc.perform(post("/api/v1/appointments")
						.header("Authorization", bearer(patientToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "doctorId": "%s",
								  "scheduledAt": "%s",
								  "durationMinutes": 30
								}
								""".formatted(doctorId, start.plus(10, ChronoUnit.MINUTES))))
				.andExpect(status().isConflict());

		mockMvc.perform(post("/api/v1/appointments/" + appointmentId + "/confirm")
						.header("Authorization", bearer(doctorToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CONFIRMED"));

		mockMvc.perform(post("/api/v1/appointments/" + appointmentId + "/cancel")
						.header("Authorization", bearer(patientToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELLED"));

		mockMvc.perform(get("/api/v1/appointments")
						.header("Authorization", bearer(patientToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(appointmentId));

		Instant later = start.plus(2, ChronoUnit.HOURS);
		MvcResult other = mockMvc.perform(post("/api/v1/appointments")
						.header("Authorization", bearer(patientToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "doctorId": "%s",
								  "scheduledAt": "%s",
								  "durationMinutes": 30
								}
								""".formatted(doctorId, later)))
				.andExpect(status().isCreated())
				.andReturn();
		String otherId = jsonId(other);
		mockMvc.perform(post("/api/v1/appointments/" + otherId + "/confirm")
						.header("Authorization", bearer(doctorToken)))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/v1/appointments/" + otherId + "/complete")
						.header("Authorization", bearer(doctorToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("COMPLETED"));
	}

	@Test
	void shouldPublishAvailabilityAndHideBookedSlots() throws Exception {
		String suffix = uniqueSuffix();
		String patientToken = registerPatient("slot.paciente." + suffix + "@vidaconecta.test", cpf(suffix, "03"));
		String doctorToken = registerDoctor("slot.medico." + suffix + "@vidaconecta.test", "CRMS" + suffix, "Dermatologia");
		String doctorId = currentUserId(doctorToken).toString();
		Instant start = Instant.now().plus(2, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS);

		mockMvc.perform(post("/api/v1/appointments")
						.header("Authorization", bearer(patientToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "doctorId": "%s",
								  "scheduledAt": "%s",
								  "durationMinutes": 30
								}
								""".formatted(doctorId, start)))
				.andExpect(status().isBadRequest());

		openClinicHours(doctorToken);

		mockMvc.perform(get("/api/v1/doctors/" + doctorId + "/slots")
						.header("Authorization", bearer(patientToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].startAt").exists())
				.andExpect(jsonPath("$[0].durationMinutes").exists());

		mockMvc.perform(post("/api/v1/me/availability")
						.header("Authorization", bearer(doctorToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "dayOfWeek": "MONDAY",
								  "startTime": "08:00",
								  "endTime": "12:00",
								  "slotMinutes": 30
								}
								"""))
				.andExpect(status().isConflict());

		mockMvc.perform(post("/api/v1/appointments")
						.header("Authorization", bearer(patientToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "doctorId": "%s",
								  "scheduledAt": "%s",
								  "durationMinutes": 30
								}
								""".formatted(doctorId, start)))
				.andExpect(status().isCreated());
	}
}
