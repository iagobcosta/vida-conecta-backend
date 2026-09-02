package br.com.vidaconecta.consent.application;

import br.com.vidaconecta.consent.api.ConsentFacade;
import br.com.vidaconecta.consent.domain.Consent;
import br.com.vidaconecta.consent.infrastructure.ConsentRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ConsentFacadeImpl implements ConsentFacade {

	private final ConsentRepository consentRepository;

	public ConsentFacadeImpl(ConsentRepository consentRepository) {
		this.consentRepository = consentRepository;
	}

	@Override
	public boolean hasValidConsent(UUID patientId, UUID doctorId, UUID appointmentId) {
		Instant now = Instant.now();
		return consentRepository.findByPatientIdAndDoctorIdAndRevokedAtIsNull(patientId, doctorId).stream()
				.filter(consent -> consent.isActive(now))
				.anyMatch(consent -> consent.covers(doctorId, appointmentId));
	}
}
