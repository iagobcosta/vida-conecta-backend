package br.com.vidaconecta.ehr.application;

import br.com.vidaconecta.consent.api.ConsentFacade;
import br.com.vidaconecta.ehr.domain.ClinicalNote;
import br.com.vidaconecta.ehr.domain.EhrAccessAudit;
import br.com.vidaconecta.ehr.infrastructure.ClinicalContentEncryptor;
import br.com.vidaconecta.ehr.infrastructure.ClinicalNoteRepository;
import br.com.vidaconecta.ehr.infrastructure.EhrAccessAuditRepository;
import br.com.vidaconecta.ehr.web.ClinicalNoteResponse;
import br.com.vidaconecta.ehr.web.CreateClinicalNoteRequest;
import br.com.vidaconecta.ehr.web.EhrAuditResponse;
import br.com.vidaconecta.identity.api.CurrentUser;
import br.com.vidaconecta.identity.api.IdentityFacade;
import br.com.vidaconecta.scheduling.api.SchedulingFacade;
import br.com.vidaconecta.shared.api.BusinessException;
import br.com.vidaconecta.shared.api.ForbiddenException;
import br.com.vidaconecta.shared.api.NotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EhrService {

	private final ClinicalNoteRepository clinicalNoteRepository;
	private final EhrAccessAuditRepository auditRepository;
	private final ClinicalContentEncryptor encryptor;
	private final ConsentFacade consentFacade;
	private final IdentityFacade identityFacade;
	private final SchedulingFacade schedulingFacade;

	public EhrService(
			ClinicalNoteRepository clinicalNoteRepository,
			EhrAccessAuditRepository auditRepository,
			ClinicalContentEncryptor encryptor,
			ConsentFacade consentFacade,
			IdentityFacade identityFacade,
			SchedulingFacade schedulingFacade) {
		this.clinicalNoteRepository = clinicalNoteRepository;
		this.auditRepository = auditRepository;
		this.encryptor = encryptor;
		this.consentFacade = consentFacade;
		this.identityFacade = identityFacade;
		this.schedulingFacade = schedulingFacade;
	}

	@Transactional
	public ClinicalNoteResponse create(CurrentUser currentUser, UUID patientId, CreateClinicalNoteRequest request) {
		if (!currentUser.isDoctor()) {
			throw new ForbiddenException("Somente médicos podem registrar evolução");
		}
		if (identityFacade.findPatient(patientId).isEmpty()) {
			throw new NotFoundException("Paciente não encontrado");
		}
		SchedulingFacade.AppointmentView appointment = schedulingFacade.findById(request.appointmentId())
				.orElseThrow(() -> new NotFoundException("Consulta não encontrada"));
		if (!appointment.doctorId().equals(currentUser.id()) || !appointment.patientId().equals(patientId)) {
			throw new ForbiddenException("A consulta não pertence a este médico e paciente");
		}
		var encrypted = encryptor.encrypt(request.content());
		ClinicalNote note = ClinicalNote.create(
				patientId,
				currentUser.id(),
				request.appointmentId(),
				encrypted.ciphertext(),
				encrypted.iv());
		clinicalNoteRepository.save(note);
		auditRepository.save(EhrAccessAudit.record(currentUser.id(), patientId, request.appointmentId(), "WRITE"));
		return toResponse(note, request.content());
	}

	@Transactional
	public List<ClinicalNoteResponse> list(CurrentUser currentUser, UUID patientId, UUID appointmentId) {
		if (identityFacade.findPatient(patientId).isEmpty()) {
			throw new NotFoundException("Paciente não encontrado");
		}
		boolean isOwner = currentUser.isPatient() && currentUser.id().equals(patientId);
		boolean isDoctor = currentUser.isDoctor();
		if (!isOwner && !isDoctor && !currentUser.isAdmin()) {
			throw new ForbiddenException("Acesso ao prontuário negado");
		}
		if (isDoctor && !currentUser.id().equals(patientId)) {
			boolean related = schedulingFacade.hasRelationship(patientId, currentUser.id());
			boolean consented = consentFacade.hasValidConsent(patientId, currentUser.id(), appointmentId);
			if (!related && !consented) {
				throw new ForbiddenException("Médico sem vínculo ou consentimento para este paciente");
			}
		}
		List<ClinicalNote> notes = clinicalNoteRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
		boolean sharedConsent = isDoctor && consentFacade.hasValidConsent(patientId, currentUser.id(), appointmentId);
		List<ClinicalNoteResponse> visible = notes.stream()
				.filter(note -> isOwner || currentUser.isAdmin() || note.authoredBy(currentUser.id()) || sharedConsent)
				.map(note -> toResponse(note, encryptor.decrypt(note.getCiphertext(), note.getIv())))
				.toList();
		auditRepository.save(EhrAccessAudit.record(currentUser.id(), patientId, appointmentId, "READ"));
		return visible;
	}

	@Transactional(readOnly = true)
	public List<EhrAuditResponse> listAudit(CurrentUser currentUser, UUID patientId) {
		if (!currentUser.isAdmin() && !(currentUser.isPatient() && currentUser.id().equals(patientId))) {
			throw new ForbiddenException("Auditoria disponível apenas ao paciente titular ou admin");
		}
		if (patientId == null) {
			throw new BusinessException("patientId é obrigatório");
		}
		return auditRepository.findByPatientIdOrderByAccessedAtDesc(patientId).stream()
				.map(this::toAudit)
				.toList();
	}

	private ClinicalNoteResponse toResponse(ClinicalNote note, String content) {
		String authorName = identityFacade.findDoctor(note.getAuthorDoctorId())
				.map(IdentityFacade.DoctorView::fullName)
				.orElse(null);
		return new ClinicalNoteResponse(
				note.getId(),
				note.getPatientId(),
				note.getAuthorDoctorId(),
				authorName,
				note.getAppointmentId(),
				content,
				note.getCreatedAt());
	}

	private EhrAuditResponse toAudit(EhrAccessAudit audit) {
		return new EhrAuditResponse(
				audit.getId(),
				audit.getActorUserId(),
				identityFacade.displayName(audit.getActorUserId()),
				audit.getPatientId(),
				audit.getAppointmentId(),
				audit.getAction(),
				audit.getAccessedAt());
	}
}
