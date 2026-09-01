package br.com.vidaconecta.consent.api;

import java.util.UUID;

public interface ConsentFacade {

	boolean hasValidConsent(UUID patientId, UUID doctorId, UUID appointmentId);
}
