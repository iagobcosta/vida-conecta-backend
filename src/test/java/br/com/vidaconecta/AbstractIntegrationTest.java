package br.com.vidaconecta;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AbstractIntegrationTest.ContainersConfig.class)
public abstract class AbstractIntegrationTest {

	@TestConfiguration(proxyBeanMethods = false)
	static class ContainersConfig {

		@Bean
		@ServiceConnection
		PostgreSQLContainer postgresContainer() {
			return new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));
		}
	}

	@Autowired
	protected MockMvc mockMvc;

	@Autowired
	protected JdbcTemplate jdbcTemplate;

	protected String uniqueSuffix() {
		return UUID.randomUUID().toString().substring(0, 8);
	}

	protected String cpf(String suffix, String tail) {
		long value = Math.abs((long) suffix.hashCode()) % 1_000_000_000L;
		return String.format("%09d%s", value, tail);
	}

	protected String registerPatient(String email, String cpf) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "%s",
								  "password": "password123",
								  "role": "PACIENTE",
								  "fullName": "Paciente Teste",
								  "cpf": "%s",
								  "birthDate": "1990-01-15",
								  "phone": "85999999999"
								}
								""".formatted(email, cpf)))
				.andExpect(status().isCreated())
				.andReturn();
		return tokenFrom(result);
	}

	protected String registerDoctor(String email, String crm, String specialty) throws Exception {
		String adminToken = registerAdmin("admin." + uniqueSuffix() + "@vidaconecta.test");
		MvcResult invite = mockMvc.perform(post("/api/v1/admin/doctors/invites")
						.header("Authorization", bearer(adminToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "%s",
								  "fullName": "Dra. %s"
								}
								""".formatted(email, specialty)))
				.andExpect(status().isCreated())
				.andReturn();
		String inviteToken = JsonPath.read(body(invite), "$.token");
		MvcResult result = mockMvc.perform(post("/api/v1/auth/register/doctor")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "token": "%s",
								  "password": "password123",
								  "crm": "%s",
								  "specialty": "%s"
								}
								""".formatted(inviteToken, crm, specialty)))
				.andExpect(status().isCreated())
				.andReturn();
		return tokenFrom(result);
	}

	protected String registerAdmin(String email) throws Exception {
		UUID bootstrap = UUID.fromString(jdbcTemplate.queryForObject("select token::text from admin_bootstrap_tokens limit 1", String.class));
		MvcResult result = mockMvc.perform(post("/api/v1/auth/register/admin")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "token": "%s",
								  "email": "%s",
								  "password": "password123",
								  "fullName": "Admin Teste"
								}
								""".formatted(bootstrap, email)))
				.andExpect(status().isCreated())
				.andReturn();
		return tokenFrom(result);
	}

	protected void openClinicHours(String doctorToken) throws Exception {
		for (DayOfWeek day : DayOfWeek.values()) {
			mockMvc.perform(post("/api/v1/me/availability")
							.header("Authorization", bearer(doctorToken))
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{
									  "dayOfWeek": "%s",
									  "startTime": "00:00",
									  "endTime": "23:59",
									  "slotMinutes": 30
									}
									""".formatted(day.name())))
					.andExpect(status().isCreated());
		}
	}

	protected String login(String email) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "%s",
								  "password": "password123"
								}
								""".formatted(email)))
				.andExpect(status().isOk())
				.andReturn();
		return tokenFrom(result);
	}

	protected UUID currentUserId(String token) throws Exception {
		MvcResult result = mockMvc.perform(get("/api/v1/auth/me").header("Authorization", bearer(token)))
				.andExpect(status().isOk())
				.andReturn();
		return UUID.fromString(JsonPath.read(body(result), "$.id"));
	}

	protected String bearer(String token) {
		return "Bearer " + token;
	}

	protected String jsonId(MvcResult result) {
		return JsonPath.read(body(result), "$.id");
	}

	private String tokenFrom(MvcResult result) {
		return JsonPath.read(body(result), "$.token");
	}

	protected String body(MvcResult result) {
		return new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
	}
}
