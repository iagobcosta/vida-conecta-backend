package br.com.vidaconecta.consent.web;

import br.com.vidaconecta.consent.application.ConsentService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/consents")
public class ConsentController {

	private final ConsentService consentService;

	public ConsentController(ConsentService consentService) {
		this.consentService = consentService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('PACIENTE')")
	public ConsentResponse grant(
			@AuthenticationPrincipal CurrentUser currentUser,
			@Valid @RequestBody GrantConsentRequest request) {
		return consentService.grant(currentUser, request);
	}

	@GetMapping
	public List<ConsentResponse> list(@AuthenticationPrincipal CurrentUser currentUser) {
		return consentService.list(currentUser);
	}

	@PostMapping("/{id}/revoke")
	@PreAuthorize("hasRole('PACIENTE')")
	public ConsentResponse revoke(
			@AuthenticationPrincipal CurrentUser currentUser,
			@PathVariable UUID id) {
		return consentService.revoke(currentUser, id);
	}
}
