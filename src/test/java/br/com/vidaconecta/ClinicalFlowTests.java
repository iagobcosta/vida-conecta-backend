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

class ClinicalFlowTests extends AbstractIntegrationTest {

	@Test
	void shouldProtectSharedEhrIssuePrescriptionAndVideoToken() throws Exception {
		String suffix = uniqueSuffix();
		String patientToken = registerPatient("flow.paciente." + suffix + "@vidaconecta.test", cpf(suffix, "11"));
		String doctorAToken = registerDoctor("flow.medica." + suffix + "@vidaconecta.test", "CRMA" + suffix, "Cardiologia");
		String doctorBToken = registerDoctor("flow.medicob." + suffix + "@vidaconecta.test", "CRMB" + suffix, "Clínica Geral");
		openClinicHours(doctorAToken);
		openClinicHours(doctorBToken);

		String patientId = currentUserId(patientToken).toString();
		String doctorAId = currentUserId(doctorAToken).toString();
		String doctorBId = currentUserId(doctorBToken).toString();
		Instant soon = Instant.now().plus(5, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.SECONDS);
		Instant later = Instant.now().plus(3, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS);

		String appointmentA = createAndConfirm(patientToken, doctorAToken, doctorAId, soon);
		String appointmentB = createAndConfirm(patientToken, doctorBToken, doctorBId, later);

		mockMvc.perform(post("/api/v1/patients/" + patientId + "/ehr")
						.header("Authorization", bearer(doctorAToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "appointmentId": "%s",
								  "content": "Paciente com histórico de hipertensão."
								}
								""".formatted(appointmentA)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.content").value("Paciente com histórico de hipertensão."));

		mockMvc.perform(get("/api/v1/patients/" + patientId + "/ehr")
						.header("Authorization", bearer(doctorBToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));

		mockMvc.perform(post("/api/v1/consents")
						.header("Authorization", bearer(patientToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "doctorId": "%s",
								  "scope": "DOCTOR"
								}
								""".formatted(doctorBId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.version").value(1))
				.andExpect(jsonPath("$.doctorName").exists())
				.andExpect(jsonPath("$.patientName").exists());

		mockMvc.perform(get("/api/v1/patients/" + patientId + "/ehr")
						.header("Authorization", bearer(doctorBToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].content").value("Paciente com histórico de hipertensão."));

		mockMvc.perform(get("/api/v1/ehr/audit").param("patientId", patientId)
						.header("Authorization", bearer(patientToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].action").exists())
				.andExpect(jsonPath("$[0].actorName").exists());

		mockMvc.perform(post("/api/v1/prescriptions")
						.header("Authorization", bearer(doctorAToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "patientId": "%s",
								  "appointmentId": "%s",
								  "items": [
								    {
								      "medication": "Losartana",
								      "dosage": "50mg",
								      "instructions": "1 comprimido pela manhã"
								    }
								  ]
								}
								""".formatted(patientId, appointmentA)))
				.andExpect(status().isCreated());

		mockMvc.perform(get("/api/v1/prescriptions").header("Authorization", bearer(patientToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].items[0].medication").value("Losartana"));

		mockMvc.perform(post("/api/v1/video/appointments/" + appointmentA + "/token")
						.header("Authorization", bearer(patientToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").exists())
				.andExpect(jsonPath("$.roomName").value("appointment-" + appointmentA));

		mockMvc.perform(post("/api/v1/video/appointments/" + appointmentB + "/token")
						.header("Authorization", bearer(doctorAToken)))
				.andExpect(status().isForbidden());
	}

	private String createAndConfirm(String patientToken, String doctorToken, String doctorId, Instant scheduledAt)
			throws Exception {
		MvcResult created = mockMvc.perform(post("/api/v1/appointments")
						.header("Authorization", bearer(patientToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "doctorId": "%s",
								  "scheduledAt": "%s",
								  "durationMinutes": 30
								}
								""".formatted(doctorId, scheduledAt)))
				.andExpect(status().isCreated())
				.andReturn();
		String appointmentId = jsonId(created);
		mockMvc.perform(post("/api/v1/appointments/" + appointmentId + "/confirm")
						.header("Authorization", bearer(doctorToken)))
				.andExpect(status().isOk());
		return appointmentId;
	}
}
