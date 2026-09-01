package br.com.vidaconecta.shared.web;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	OpenAPI vidaConectaOpenApi() {
		SecurityScheme bearer = new SecurityScheme()
				.type(SecurityScheme.Type.HTTP)
				.scheme("bearer")
				.bearerFormat("JWT");
		return new OpenAPI()
				.info(new Info()
						.title("Vida Conecta API")
						.version("v1")
						.description("API do monólito modular Vida Conecta"))
				.components(new Components().addSecuritySchemes("bearer-jwt", bearer))
				.addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
	}
}
