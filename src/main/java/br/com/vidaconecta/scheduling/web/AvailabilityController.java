package br.com.vidaconecta.scheduling.web;

import br.com.vidaconecta.identity.api.CurrentUser;
import br.com.vidaconecta.scheduling.application.AvailabilityService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class AvailabilityController {

	private final AvailabilityService availabilityService;

	public AvailabilityController(AvailabilityService availabilityService) {
		this.availabilityService = availabilityService;
	}

	@GetMapping("/me/availability")
	@PreAuthorize("hasRole('MEDICO')")
	public List<AvailabilityResponse> mine(@AuthenticationPrincipal CurrentUser currentUser) {
		return availabilityService.listMine(currentUser);
	}

	@PostMapping("/me/availability")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('MEDICO')")
	public AvailabilityResponse create(
			@AuthenticationPrincipal CurrentUser currentUser,
			@Valid @RequestBody CreateAvailabilityRequest request) {
		return availabilityService.create(currentUser, request);
	}

	@DeleteMapping("/me/availability/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("hasRole('MEDICO')")
	public void delete(
			@AuthenticationPrincipal CurrentUser currentUser,
			@PathVariable UUID id) {
		availabilityService.delete(currentUser, id);
	}

	@GetMapping("/doctors/{doctorId}/availability")
	public List<AvailabilityResponse> byDoctor(@PathVariable UUID doctorId) {
		return availabilityService.listByDoctor(doctorId);
	}

	@GetMapping("/doctors/{doctorId}/slots")
	public List<AvailableSlotResponse> slots(@PathVariable UUID doctorId) {
		return availabilityService.listSlots(doctorId, Instant.now());
	}
}
