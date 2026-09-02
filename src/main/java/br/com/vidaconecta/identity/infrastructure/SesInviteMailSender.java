package br.com.vidaconecta.identity.infrastructure;

import br.com.vidaconecta.identity.application.InviteMailSender;
import br.com.vidaconecta.identity.application.MailProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;

public class SesInviteMailSender implements InviteMailSender {

	private static final Logger log = LoggerFactory.getLogger(SesInviteMailSender.class);

	private final SesClient sesClient;
	private final MailProperties mailProperties;

	public SesInviteMailSender(SesClient sesClient, MailProperties mailProperties) {
		this.sesClient = sesClient;
		this.mailProperties = mailProperties;
	}

	@Override
	public void sendDoctorInvite(String to, String doctorName, String inviteUrl) {
		String subject = "Convite para a equipe Vida Conecta";
		String html = """
				<p>Olá, %s.</p>
				<p>Você foi convidado(a) para atuar como médico(a) na plataforma Vida Conecta.</p>
				<p>Para concluir o cadastro (CRM, especialidade e senha), acesse:</p>
				<p><a href="%s">%s</a></p>
				<p>Se você não esperava este convite, ignore esta mensagem.</p>
				""".formatted(doctorName, inviteUrl, inviteUrl);
		try {
			sesClient.sendEmail(SendEmailRequest.builder()
					.source(mailProperties.from())
					.destination(Destination.builder().toAddresses(to).build())
					.message(Message.builder()
							.subject(Content.builder().data(subject).charset("UTF-8").build())
							.body(Body.builder()
									.html(Content.builder().data(html).charset("UTF-8").build())
									.build())
							.build())
					.build());
		} catch (RuntimeException exception) {
			log.error("Falha ao enviar convite via SES para {}", to, exception);
			throw exception;
		}
	}
}
