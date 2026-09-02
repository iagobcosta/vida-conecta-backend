package br.com.vidaconecta.identity.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "vida-conecta.mail")
public record MailProperties(
		@DefaultValue("noreply@localhost") String from,
		@DefaultValue("http://localhost:5173") String frontendBaseUrl,
		@DefaultValue("false") boolean sesEnabled,
		@DefaultValue("us-east-1") String awsRegion) {
}
