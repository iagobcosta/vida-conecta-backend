package br.com.vidaconecta.prescription.web;

import br.com.vidaconecta.identity.api.CurrentUser;
import br.com.vidaconecta.prescription.application.PrescriptionService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/prescriptions")
public class PrescriptionController {

	private final PrescriptionService prescriptionService;

	public PrescriptionController(PrescriptionService prescriptionService) {
		this.prescriptionService = prescriptionService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('MEDICO')")
	public PrescriptionResponse create(
			@AuthenticationPrincipal CurrentUser currentUser,
			@Valid @RequestBody CreatePrescriptionRequest request) {
		return prescriptionService.create(currentUser, request);
	}

	@GetMapping
	public List<PrescriptionResponse> list(@AuthenticationPrincipal CurrentUser currentUser) {
		return prescriptionService.list(currentUser);
	}

	@GetMapping("/{id}")
	public PrescriptionResponse get(
			@AuthenticationPrincipal CurrentUser currentUser,
			@PathVariable UUID id) {
		return prescriptionService.get(currentUser, id);
	}
}
