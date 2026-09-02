package br.com.vidaconecta.consent.application;

import br.com.vidaconecta.consent.api.ConsentScope;
import br.com.vidaconecta.consent.domain.Consent;
import br.com.vidaconecta.consent.infrastructure.ConsentRepository;
import br.com.vidaconecta.consent.web.ConsentResponse;
import br.com.vidaconecta.consent.web.GrantConsentRequest;
import br.com.vidaconecta.identity.api.CurrentUser;
import br.com.vidaconecta.identity.api.IdentityFacade;
import br.com.vidaconecta.notification.api.NotificationFacade;
import br.com.vidaconecta.notification.api.NotificationType;
import br.com.vidaconecta.shared.api.BusinessException;
import br.com.vidaconecta.shared.api.ForbiddenException;
import br.com.vidaconecta.shared.api.NotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsentService {

	private final ConsentRepository consentRepository;
	private final IdentityFacade identityFacade;
	private final NotificationFacade notificationFacade;

	public ConsentService(
			ConsentRepository consentRepository,
			IdentityFacade identityFacade,
			NotificationFacade notificationFacade) {
		this.consentRepository = consentRepository;
		this.identityFacade = identityFacade;
		this.notificationFacade = notificationFacade;
	}

	@Transactional
	public ConsentResponse grant(CurrentUser currentUser, GrantConsentRequest request) {
		if (!currentUser.isPatient()) {
			throw new ForbiddenException("Somente o paciente pode conceder consentimento");
		}
		if (!identityFacade.isDoctor(request.doctorId())) {
			throw new NotFoundException("Médico não encontrado");
		}
		if (request.scope() == ConsentScope.APPOINTMENT && request.appointmentId() == null) {
			throw new BusinessException("Consentimento por consulta exige appointmentId");
		}
		if (request.scope() == ConsentScope.DOCTOR && request.appointmentId() != null) {
			throw new BusinessException("Consentimento por médico não deve informar appointmentId");
		}
		int version = consentRepository.nextVersionBase(currentUser.id(), request.doctorId()) + 1;
		Consent consent = Consent.grant(
				currentUser.id(),
				request.doctorId(),
				request.scope(),
				request.appointmentId(),
				version,
				request.expiresAt());
		consentRepository.save(consent);
		String patientName = identityFacade.displayName(currentUser.id());
		notificationFacade.push(new NotificationFacade.NewNotification(
				request.doctorId(),
				NotificationType.CONSENT_GRANTED,
				"Consentimento concedido",
				patientName + " autorizou o acesso ao prontuário.",
				request.appointmentId(),
				"/prontuario",
				"Abrir prontuário"));
		return toResponse(consent);
	}

	@Transactional(readOnly = true)
	public List<ConsentResponse> list(CurrentUser currentUser) {
		List<Consent> consents = currentUser.isDoctor()
				? consentRepository.findByDoctorIdOrderByGrantedAtDesc(currentUser.id())
				: consentRepository.findByPatientIdOrderByGrantedAtDesc(currentUser.id());
		return consents.stream().map(this::toResponse).toList();
	}

	@Transactional
	public ConsentResponse revoke(CurrentUser currentUser, UUID consentId) {
		Consent consent = consentRepository.findById(consentId)
				.orElseThrow(() -> new NotFoundException("Consentimento não encontrado"));
		if (!consent.getPatientId().equals(currentUser.id())) {
			throw new ForbiddenException("Somente o paciente titular pode revogar o consentimento");
		}
		try {
			consent.revoke();
		} catch (IllegalStateException exception) {
			throw new BusinessException(exception.getMessage());
		}
		String patientName = identityFacade.displayName(currentUser.id());
		notificationFacade.push(new NotificationFacade.NewNotification(
				consent.getDoctorId(),
				NotificationType.CONSENT_REVOKED,
				"Consentimento revogado",
				patientName + " revogou o acesso ao prontuário.",
				consent.getAppointmentId(),
				"/prontuario",
				"Ver prontuário"));
		return toResponse(consent);
	}

	private ConsentResponse toResponse(Consent consent) {
		return new ConsentResponse(
				consent.getId(),
				consent.getPatientId(),
				identityFacade.findPatient(consent.getPatientId()).map(IdentityFacade.PatientView::fullName).orElse(null),
				consent.getDoctorId(),
				identityFacade.findDoctor(consent.getDoctorId()).map(IdentityFacade.DoctorView::fullName).orElse(null),
				consent.getScope(),
				consent.getAppointmentId(),
				consent.getVersion(),
				consent.getGrantedAt(),
				consent.getExpiresAt(),
				consent.getRevokedAt());
	}
}
