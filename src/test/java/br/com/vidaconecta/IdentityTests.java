package br.com.vidaconecta;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
}
