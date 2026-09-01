package br.com.vidaconecta.scheduling.web;

import br.com.vidaconecta.identity.api.CurrentUser;
import br.com.vidaconecta.scheduling.application.AppointmentService;
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
@RequestMapping("/api/v1")
public class AppointmentController {

	private final AppointmentService appointmentService;

	public AppointmentController(AppointmentService appointmentService) {
		this.appointmentService = appointmentService;
	}

	@GetMapping("/doctors")
	public List<DoctorResponse> listDoctors() {
		return appointmentService.listDoctors();
	}

	@PostMapping("/appointments")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('PACIENTE')")
	public AppointmentResponse create(
			@AuthenticationPrincipal CurrentUser currentUser,
			@Valid @RequestBody CreateAppointmentRequest request) {
		return appointmentService.create(currentUser, request);
	}

	@GetMapping("/appointments")
	public List<AppointmentResponse> list(@AuthenticationPrincipal CurrentUser currentUser) {
		return appointmentService.list(currentUser);
	}

	@GetMapping("/appointments/{id}")
	public AppointmentResponse get(
			@AuthenticationPrincipal CurrentUser currentUser,
			@PathVariable UUID id) {
		return appointmentService.get(currentUser, id);
	}

	@PostMapping("/appointments/{id}/confirm")
	@PreAuthorize("hasRole('MEDICO')")
	public AppointmentResponse confirm(
			@AuthenticationPrincipal CurrentUser currentUser,
			@PathVariable UUID id) {
		return appointmentService.confirm(currentUser, id);
	}

	@PostMapping("/appointments/{id}/cancel")
	public AppointmentResponse cancel(
			@AuthenticationPrincipal CurrentUser currentUser,
			@PathVariable UUID id) {
		return appointmentService.cancel(currentUser, id);
	}
}
