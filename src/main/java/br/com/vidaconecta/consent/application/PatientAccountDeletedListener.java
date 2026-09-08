package br.com.vidaconecta.consent.application;

import br.com.vidaconecta.consent.api.ConsentFacade;
import br.com.vidaconecta.identity.api.PatientAccountDeleted;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
class PatientAccountDeletedListener {

	private final ConsentFacade consentFacade;

	PatientAccountDeletedListener(ConsentFacade consentFacade) {
		this.consentFacade = consentFacade;
	}

	@EventListener
	void onPatientAccountDeleted(PatientAccountDeleted event) {
		consentFacade.revokeAllFromPatient(event.patientId());
	}
}
