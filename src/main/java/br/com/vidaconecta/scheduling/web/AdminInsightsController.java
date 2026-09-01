package br.com.vidaconecta.scheduling.web;

import br.com.vidaconecta.scheduling.application.AdminInsightsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminInsightsController {

	private final AdminInsightsService adminInsightsService;

	public AdminInsightsController(AdminInsightsService adminInsightsService) {
		this.adminInsightsService = adminInsightsService;
	}

	@GetMapping("/insights")
	public AdminInsightsResponse insights() {
		return adminInsightsService.insights();
	}
}
