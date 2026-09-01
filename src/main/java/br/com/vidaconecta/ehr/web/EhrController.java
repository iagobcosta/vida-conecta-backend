package br.com.vidaconecta.ehr.web;

import br.com.vidaconecta.ehr.application.EhrService;
import br.com.vidaconecta.ehr.domain.EhrAccessAudit;
import br.com.vidaconecta.identity.api.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class EhrController {

	private final EhrService ehrService;

	public EhrController(EhrService ehrService) {
		this.ehrService = ehrService;
	}

	@PostMapping("/patients/{patientId}/ehr")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('MEDICO')")
	public ClinicalNoteResponse create(
			@AuthenticationPrincipal CurrentUser currentUser,
			@PathVariable UUID patientId,
			@Valid @RequestBody CreateClinicalNoteRequest request) {
		return ehrService.create(currentUser, patientId, request);
	}

	@GetMapping("/patients/{patientId}/ehr")
	public List<ClinicalNoteResponse> list(
			@AuthenticationPrincipal CurrentUser currentUser,
			@PathVariable UUID patientId,
			@RequestParam(required = false) UUID appointmentId) {
		return ehrService.list(currentUser, patientId, appointmentId);
	}

	@GetMapping("/ehr/audit")
	public List<EhrAuditResponse> audit(
			@AuthenticationPrincipal CurrentUser currentUser,
			@RequestParam UUID patientId) {
		return ehrService.listAudit(currentUser, patientId).stream()
				.map(EhrController::toAudit)
				.toList();
	}

	private static EhrAuditResponse toAudit(EhrAccessAudit audit) {
		return new EhrAuditResponse(
				audit.getId(),
				audit.getActorUserId(),
				audit.getPatientId(),
				audit.getAppointmentId(),
				audit.getAction(),
				audit.getAccessedAt());
	}
}
