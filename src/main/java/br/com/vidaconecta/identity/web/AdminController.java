package br.com.vidaconecta.identity.web;

import br.com.vidaconecta.identity.api.CurrentUser;
import br.com.vidaconecta.identity.application.AdminStaffService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

	private final AdminStaffService adminStaffService;

	public AdminController(AdminStaffService adminStaffService) {
		this.adminStaffService = adminStaffService;
	}

	@GetMapping("/bootstrap-token")
	public BootstrapTokenResponse bootstrapToken() {
		return adminStaffService.currentBootstrapToken();
	}

	@PostMapping("/doctors/invites")
	@ResponseStatus(HttpStatus.CREATED)
	public DoctorInviteResponse inviteDoctor(
			@AuthenticationPrincipal CurrentUser currentUser,
			@Valid @RequestBody InviteDoctorRequest request) {
		return adminStaffService.inviteDoctor(currentUser, request);
	}

	@GetMapping("/doctors/invites")
	public List<DoctorInviteResponse> listInvites() {
		return adminStaffService.listInvites();
	}

	@GetMapping("/doctors")
	public List<ManagedDoctorResponse> listDoctors() {
		return adminStaffService.listDoctors();
	}
}
