package br.com.vidaconecta;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

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
}
