package br.com.vidaconecta.portability.web;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.vidaconecta.identity.api.CurrentUser;
import br.com.vidaconecta.portability.application.PortabilityService;

@RestController
@RequestMapping("/api/v1/auth")
public class PortabilityController {

	private final PortabilityService portabilityService;

	public PortabilityController(PortabilityService portabilityService) {
		this.portabilityService = portabilityService;
	}

	@GetMapping("/me/export")
	public DataExportResponse exportData(@AuthenticationPrincipal CurrentUser currentUser, Authentication auth) {

		if (currentUser == null) {
            throw new RuntimeException("Current user is null! Auth: " + (auth != null ? auth.getClass().getName() : "null"));
        }

		if (!currentUser.isPatient()) {
			throw new org.springframework.security.access.AccessDeniedException("Apenas pacientes podem exportar dados.");
		}

		return portabilityService.exportPatientData(currentUser);
	}
}
