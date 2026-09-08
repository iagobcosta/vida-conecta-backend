package br.com.vidaconecta;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class IdentityTests extends AbstractIntegrationTest {

	@Test
	void shouldRegisterLoginAndReturnCurrentUser() throws Exception {
		String suffix = uniqueSuffix();
		String email = "paciente." + suffix + "@vidaconecta.test";
		String token = registerPatient(email, cpf(suffix, "01"));

		mockMvc.perform(get("/api/v1/auth/me").header("Authorization", bearer(token)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(email))
				.andExpect(jsonPath("$.role").value("PACIENTE"))
				.andExpect(jsonPath("$.fullName").value("Paciente Teste"))
				.andExpect(jsonPath("$.cpf").value(startsWith("***.***.***-")));

		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "%s",
								  "password": "password123"
								}
								""".formatted(email)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").exists());
	}

	@Test
	void shouldRejectDuplicateEmail() throws Exception {
		String suffix = uniqueSuffix();
		String email = "dup." + suffix + "@vidaconecta.test";
		registerPatient(email, cpf(suffix, "01"));

		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "%s",
								  "password": "password123",
								  "role": "PACIENTE",
								  "fullName": "Outro",
								  "cpf": "%s",
								  "birthDate": "1991-02-02"
								}
								""".formatted(email, cpf(suffix, "02"))))
				.andExpect(status().isConflict());
	}

	@Test
	void shouldRejectPublicDoctorAndAdminRegister() throws Exception {
		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "medico.publico@vidaconecta.test",
								  "password": "password123",
								  "role": "MEDICO",
								  "fullName": "Médico Público",
								  "crm": "CRM000",
								  "specialty": "Clínica"
								}
								"""))
				.andExpect(status().isBadRequest());

		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "admin.publico@vidaconecta.test",
								  "password": "password123",
								  "role": "ADMIN",
								  "fullName": "Admin Público"
								}
								"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRegisterAdminRotatingBootstrapTokenAndInviteDoctor() throws Exception {
		String suffix = uniqueSuffix();
		UUID firstToken = UUID.fromString(jdbcTemplate.queryForObject("select token::text from admin_bootstrap_tokens limit 1", String.class));

		MvcResult adminCreated = mockMvc.perform(post("/api/v1/auth/register/admin")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "token": "%s",
								  "email": "admin.%s@vidaconecta.test",
								  "password": "password123",
								  "fullName": "Admin Vida"
								}
								""".formatted(firstToken, suffix)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.token").exists())
				.andExpect(jsonPath("$.nextBootstrapToken").exists())
				.andReturn();
		String adminJwt = JsonPath.read(body(adminCreated), "$.token");
		String nextToken = JsonPath.read(body(adminCreated), "$.nextBootstrapToken");

		mockMvc.perform(post("/api/v1/auth/register/admin")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "token": "%s",
								  "email": "admin.reuso.%s@vidaconecta.test",
								  "password": "password123",
								  "fullName": "Outro Admin"
								}
								""".formatted(firstToken, suffix)))
				.andExpect(status().isBadRequest());

		mockMvc.perform(get("/api/v1/admin/bootstrap-token").header("Authorization", bearer(adminJwt)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").value(nextToken));

		String doctorEmail = "convite." + suffix + "@vidaconecta.test";
		MvcResult invite = mockMvc.perform(post("/api/v1/admin/doctors/invites")
						.header("Authorization", bearer(adminJwt))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "%s",
								  "fullName": "Dra. Convite"
								}
								""".formatted(doctorEmail)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("PENDING"))
				.andExpect(jsonPath("$.token").exists())
				.andExpect(jsonPath("$.inviteUrl").exists())
				.andReturn();
		String inviteToken = JsonPath.read(body(invite), "$.token");

		mockMvc.perform(get("/api/v1/auth/invites/" + inviteToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(doctorEmail))
				.andExpect(jsonPath("$.fullName").value("Dra. Convite"));

		mockMvc.perform(post("/api/v1/auth/register/doctor")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "token": "%s",
								  "password": "password123",
								  "crm": "CRMI%s",
								  "specialty": "Pediatria"
								}
								""".formatted(inviteToken, suffix)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.token").exists());

		mockMvc.perform(get("/api/v1/admin/doctors").header("Authorization", bearer(adminJwt)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].email", hasItem(doctorEmail)));
	}

	@Test
	void shouldDisableDoctorHidingFromPublicListingAndBlockingLogin() throws Exception {
		String suffix = uniqueSuffix();
		String adminToken = registerAdmin("admin.status." + suffix + "@vidaconecta.test");
		String doctorEmail = "medico.status." + suffix + "@vidaconecta.test";
		String doctorToken = registerDoctor(doctorEmail, "CRMS" + suffix, "Clínica Geral");
		String doctorId = currentUserId(doctorToken).toString();
		String patientToken = registerPatient("pac.status." + suffix + "@vidaconecta.test", cpf(suffix, "09"));

		mockMvc.perform(get("/api/v1/doctors").header("Authorization", bearer(patientToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].id", hasItem(doctorId)));

		mockMvc.perform(patch("/api/v1/admin/doctors/" + doctorId + "/enabled")
						.header("Authorization", bearer(adminToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "enabled": false }
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.enabled").value(false));

		mockMvc.perform(get("/api/v1/doctors").header("Authorization", bearer(patientToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].id", not(hasItem(doctorId))));

		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "%s",
								  "password": "password123"
								}
								""".formatted(doctorEmail)))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(get("/api/v1/auth/me").header("Authorization", bearer(doctorToken)))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(patch("/api/v1/admin/doctors/" + doctorId + "/enabled")
						.header("Authorization", bearer(adminToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "enabled": true }
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.enabled").value(true));

		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "%s",
								  "password": "password123"
								}
								""".formatted(doctorEmail)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").exists());
	}

	@Test
	void shouldUpdateProfilesForPatientDoctorAndAdmin() throws Exception {
		String suffix = uniqueSuffix();
		String patientToken = registerPatient("upd.pac." + suffix + "@vidaconecta.test", cpf(suffix, "21"));
		String doctorToken = registerDoctor("upd.med." + suffix + "@vidaconecta.test", "CRMU" + suffix, "Clínica Geral");
		String adminToken = registerAdmin("upd.adm." + suffix + "@vidaconecta.test");

		mockMvc.perform(patch("/api/v1/auth/me")
						.header("Authorization", bearer(patientToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "fullName": "Paciente Atualizado",
								  "phone": "85988887777",
								  "birthDate": "1988-05-20"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.fullName").value("Paciente Atualizado"))
				.andExpect(jsonPath("$.phone").value("85988887777"))
				.andExpect(jsonPath("$.birthDate").value("1988-05-20"));

		mockMvc.perform(patch("/api/v1/auth/me")
						.header("Authorization", bearer(doctorToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "fullName": "Dra. Atualizada",
								  "specialty": "Dermatologia"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.fullName").value("Dra. Atualizada"))
				.andExpect(jsonPath("$.specialty").value("Dermatologia"));

		mockMvc.perform(patch("/api/v1/auth/me")
						.header("Authorization", bearer(adminToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "fullName": "Admin Atualizado" }
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.fullName").value("Admin Atualizado"));
	}

	@Test
	void shouldAnonymizePatientAccountAndRevokeConsents() throws Exception {
		String suffix = uniqueSuffix();
		String patientEmail = "del.pac." + suffix + "@vidaconecta.test";
		String patientToken = registerPatient(patientEmail, cpf(suffix, "31"));
		String doctorToken = registerDoctor("del.med." + suffix + "@vidaconecta.test", "CRMD" + suffix, "Pediatria");
		String doctorId = currentUserId(doctorToken).toString();

		mockMvc.perform(post("/api/v1/consents")
						.header("Authorization", bearer(patientToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "doctorId": "%s",
								  "scope": "DOCTOR"
								}
								""".formatted(doctorId)))
				.andExpect(status().isCreated());

		mockMvc.perform(delete("/api/v1/auth/me").header("Authorization", bearer(doctorToken)))
				.andExpect(status().isBadRequest());

		mockMvc.perform(delete("/api/v1/auth/me").header("Authorization", bearer(patientToken)))
				.andExpect(status().isNoContent());

		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "%s",
								  "password": "password123"
								}
								""".formatted(patientEmail)))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(get("/api/v1/auth/me").header("Authorization", bearer(patientToken)))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(get("/api/v1/consents").header("Authorization", bearer(doctorToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].revokedAt").exists());
	}

	@Test
	void shouldRejectInvalidPatientRegisterAndWrongPassword() throws Exception {
		String suffix = uniqueSuffix();
		String email = "val." + suffix + "@vidaconecta.test";

		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "%s",
								  "password": "password123",
								  "role": "PACIENTE",
								  "fullName": "Sem CPF"
								}
								""".formatted(email)))
				.andExpect(status().isBadRequest());

		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "%s",
								  "password": "password123",
								  "role": "PACIENTE",
								  "fullName": "CPF curto",
								  "cpf": "123456789",
								  "birthDate": "1990-01-01"
								}
								""".formatted(email)))
				.andExpect(status().isBadRequest());

		String firstCpf = cpf(suffix, "41");
		registerPatient(email, firstCpf);

		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "outro.%s@vidaconecta.test",
								  "password": "password123",
								  "role": "PACIENTE",
								  "fullName": "Mesmo CPF",
								  "cpf": "%s",
								  "birthDate": "1992-03-03"
								}
								""".formatted(suffix, firstCpf)))
				.andExpect(status().isConflict());

		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "%s",
								  "password": "senha-errada"
								}
								""".formatted(email)))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void shouldRefreshDoctorInviteAndRejectUsedToken() throws Exception {
		String suffix = uniqueSuffix();
		String adminToken = registerAdmin("inv.adm." + suffix + "@vidaconecta.test");
		String doctorEmail = "inv.med." + suffix + "@vidaconecta.test";

		MvcResult first = mockMvc.perform(post("/api/v1/admin/doctors/invites")
						.header("Authorization", bearer(adminToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "%s",
								  "fullName": "Dra. Primeiro Nome"
								}
								""".formatted(doctorEmail)))
				.andExpect(status().isCreated())
				.andReturn();
		String firstInviteToken = JsonPath.read(body(first), "$.token");

		MvcResult refreshed = mockMvc.perform(post("/api/v1/admin/doctors/invites")
						.header("Authorization", bearer(adminToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "%s",
								  "fullName": "Dra. Nome Atualizado"
								}
								""".formatted(doctorEmail)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("PENDING"))
				.andExpect(jsonPath("$.fullName").value("Dra. Nome Atualizado"))
				.andReturn();
		String inviteToken = JsonPath.read(body(refreshed), "$.token");

		mockMvc.perform(get("/api/v1/admin/doctors/invites").header("Authorization", bearer(adminToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].email", hasItem(doctorEmail)));

		mockMvc.perform(post("/api/v1/auth/register/doctor")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "token": "%s",
								  "password": "password123",
								  "crm": "CRMR%s",
								  "specialty": "Ortopedia"
								}
								""".formatted(inviteToken, suffix)))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/auth/register/doctor")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "token": "%s",
								  "password": "password123",
								  "crm": "CRMX%s",
								  "specialty": "Ortopedia"
								}
								""".formatted(inviteToken, suffix)))
				.andExpect(status().isBadRequest());

		mockMvc.perform(get("/api/v1/auth/invites/" + firstInviteToken))
				.andExpect(status().isNotFound());

		String patientId = currentUserId(registerPatient("inv.pac." + suffix + "@vidaconecta.test", cpf(suffix, "51"))).toString();
		mockMvc.perform(patch("/api/v1/admin/doctors/" + patientId + "/enabled")
						.header("Authorization", bearer(adminToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "enabled": false }
								"""))
				.andExpect(status().isBadRequest());
	}
}
