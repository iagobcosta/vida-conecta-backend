package br.com.vidaconecta.identity.web;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.vidaconecta.identity.api.CurrentUser;
import br.com.vidaconecta.identity.application.AdminStaffService;
import br.com.vidaconecta.identity.application.AuthService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthService authService;
	private final AdminStaffService adminStaffService;

	public AuthController(AuthService authService, AdminStaffService adminStaffService) {
		this.authService = authService;
		this.adminStaffService = adminStaffService;
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	public TokenResponse register(@Valid @RequestBody RegisterRequest request) {
		return authService.register(request);
	}

	@PostMapping("/register/admin")
	@ResponseStatus(HttpStatus.CREATED)
	public AdminRegisterResponse registerAdmin(@Valid @RequestBody RegisterAdminRequest request) {
		return adminStaffService.registerAdmin(request);
	}

	@PostMapping("/register/doctor")
	@ResponseStatus(HttpStatus.CREATED)
	public TokenResponse registerDoctor(@Valid @RequestBody CompleteDoctorRequest request) {
		return adminStaffService.completeDoctor(request);
	}

	@GetMapping("/invites/{token}")
	public DoctorInvitePreviewResponse previewInvite(@PathVariable UUID token) {
		return adminStaffService.previewInvite(token);
	}

	@PostMapping("/login")
	public TokenResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request);
	}

	@GetMapping("/me")
	public MeResponse me(@AuthenticationPrincipal CurrentUser currentUser) {
		return authService.me(currentUser);
	}

}
