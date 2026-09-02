package br.com.vidaconecta.identity.web;

import br.com.vidaconecta.identity.application.InviteMailSender;
import br.com.vidaconecta.identity.application.MailProperties;
import br.com.vidaconecta.identity.infrastructure.LoggingInviteMailSender;
import br.com.vidaconecta.identity.infrastructure.SesInviteMailSender;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;

@Configuration
public class MailConfig {

	@Bean
	@ConditionalOnProperty(prefix = "vida-conecta.mail", name = "ses-enabled", havingValue = "true")
	SesClient sesClient(MailProperties properties) {
		return SesClient.builder().region(Region.of(properties.awsRegion())).build();
	}

	@Bean
	@ConditionalOnProperty(prefix = "vida-conecta.mail", name = "ses-enabled", havingValue = "true")
	InviteMailSender sesInviteMailSender(SesClient sesClient, MailProperties properties) {
		return new SesInviteMailSender(sesClient, properties);
	}

	@Bean
	@ConditionalOnMissingBean(InviteMailSender.class)
	InviteMailSender loggingInviteMailSender() {
		return new LoggingInviteMailSender();
	}
}
