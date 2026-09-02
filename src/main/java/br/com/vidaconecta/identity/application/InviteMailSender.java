package br.com.vidaconecta.identity.application;

public interface InviteMailSender {

	void sendDoctorInvite(String to, String doctorName, String inviteUrl);
}
