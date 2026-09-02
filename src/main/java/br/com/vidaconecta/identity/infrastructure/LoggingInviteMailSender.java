package br.com.vidaconecta.identity.infrastructure;

import br.com.vidaconecta.identity.application.InviteMailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingInviteMailSender implements InviteMailSender {

	private static final Logger log = LoggerFactory.getLogger(LoggingInviteMailSender.class);

	@Override
	public void sendDoctorInvite(String to, String doctorName, String inviteUrl) {
		log.info("Convite de médico para {} <{}>: {}", doctorName, to, inviteUrl);
	}
}
