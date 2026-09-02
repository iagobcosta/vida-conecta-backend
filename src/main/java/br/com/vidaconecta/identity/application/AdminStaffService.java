package br.com.vidaconecta.identity.application;

import br.com.vidaconecta.identity.api.CurrentUser;
import br.com.vidaconecta.identity.api.Role;
import br.com.vidaconecta.identity.domain.AdminBootstrapToken;
import br.com.vidaconecta.identity.domain.AdminProfile;
import br.com.vidaconecta.identity.domain.DoctorInvite;
import br.com.vidaconecta.identity.domain.DoctorProfile;
import br.com.vidaconecta.identity.domain.User;
import br.com.vidaconecta.identity.infrastructure.AdminBootstrapTokenRepository;
import br.com.vidaconecta.identity.infrastructure.AdminProfileRepository;
import br.com.vidaconecta.identity.infrastructure.DoctorInviteRepository;
import br.com.vidaconecta.identity.infrastructure.DoctorProfileRepository;
import br.com.vidaconecta.identity.infrastructure.UserRepository;
import br.com.vidaconecta.identity.web.AdminRegisterResponse;
import br.com.vidaconecta.identity.web.BootstrapTokenResponse;
import br.com.vidaconecta.identity.web.CompleteDoctorRequest;
import br.com.vidaconecta.identity.web.DoctorInvitePreviewResponse;
import br.com.vidaconecta.identity.web.DoctorInviteResponse;
import br.com.vidaconecta.identity.web.InviteDoctorRequest;
import br.com.vidaconecta.identity.web.ManagedDoctorResponse;
import br.com.vidaconecta.identity.web.RegisterAdminRequest;
import br.com.vidaconecta.identity.web.TokenResponse;
import br.com.vidaconecta.shared.api.BusinessException;
import br.com.vidaconecta.shared.api.ConflictException;
import br.com.vidaconecta.shared.api.NotFoundException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminStaffService {

	private static final Logger log = LoggerFactory.getLogger(AdminStaffService.class);
	private static final Duration INVITE_TTL = Duration.ofDays(7);

	private final UserRepository userRepository;
	private final AdminBootstrapTokenRepository bootstrapTokenRepository;
	private final AdminProfileRepository adminProfileRepository;
	private final DoctorInviteRepository doctorInviteRepository;
	private final DoctorProfileRepository doctorProfileRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final InviteMailSender inviteMailSender;
	private final MailProperties mailProperties;

	public AdminStaffService(
			UserRepository userRepository,
			AdminBootstrapTokenRepository bootstrapTokenRepository,
			AdminProfileRepository adminProfileRepository,
			DoctorInviteRepository doctorInviteRepository,
			DoctorProfileRepository doctorProfileRepository,
			PasswordEncoder passwordEncoder,
			JwtService jwtService,
			InviteMailSender inviteMailSender,
			MailProperties mailProperties) {
		this.userRepository = userRepository;
		this.bootstrapTokenRepository = bootstrapTokenRepository;
		this.adminProfileRepository = adminProfileRepository;
		this.doctorInviteRepository = doctorInviteRepository;
		this.doctorProfileRepository = doctorProfileRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.inviteMailSender = inviteMailSender;
		this.mailProperties = mailProperties;
	}

	@Transactional
	public AdminRegisterResponse registerAdmin(RegisterAdminRequest request) {
		UUID nextToken = consumeAndRotate(request.token());
		String email = request.email().trim().toLowerCase(Locale.ROOT);
		if (userRepository.existsByEmailIgnoreCase(email)) {
			throw new ConflictException("E-mail já cadastrado");
		}
		User user = User.create(email, passwordEncoder.encode(request.password()), Role.ADMIN);
		userRepository.saveAndFlush(user);
		AdminProfile profile = AdminProfile.of(user, request.fullName().trim());
		adminProfileRepository.save(profile);
		user.attachAdminProfile(profile);
		return new AdminRegisterResponse(jwtService.issueToken(user), nextToken);
	}

	@Transactional(readOnly = true)
	public BootstrapTokenResponse currentBootstrapToken() {
		AdminBootstrapToken current = bootstrapTokenRepository.findFirstByOrderByCreatedAtAsc()
				.orElseThrow(() -> new NotFoundException("Token de cadastro de admin não encontrado"));
		return new BootstrapTokenResponse(current.getToken());
	}

	@Transactional
	public DoctorInviteResponse inviteDoctor(CurrentUser currentUser, InviteDoctorRequest request) {
		String email = request.email().trim().toLowerCase(Locale.ROOT);
		if (userRepository.existsByEmailIgnoreCase(email)) {
			throw new ConflictException("Este e-mail já possui conta no sistema");
		}
		Instant expiresAt = Instant.now().plus(INVITE_TTL);
		String fullName = request.fullName().trim();
		DoctorInvite invite = doctorInviteRepository.findByEmailIgnoreCaseAndAcceptedAtIsNull(email)
				.map(existing -> {
					existing.refresh(fullName, expiresAt);
					return existing;
				})
				.orElseGet(() -> DoctorInvite.create(email, fullName, currentUser.id(), expiresAt));
		doctorInviteRepository.save(invite);
		String inviteUrl = doctorInviteUrl(invite.getToken());
		try {
			inviteMailSender.sendDoctorInvite(email, invite.getFullName(), inviteUrl);
		} catch (RuntimeException exception) {
			log.warn("Convite persistido, mas o e-mail não foi enviado para {}", email, exception);
		}
		return toInviteResponse(invite, inviteUrl);
	}

	@Transactional(readOnly = true)
	public DoctorInvitePreviewResponse previewInvite(UUID token) {
		DoctorInvite invite = requireUsableInvite(token);
		return new DoctorInvitePreviewResponse(invite.getEmail(), invite.getFullName(), invite.getExpiresAt());
	}

	@Transactional
	public TokenResponse completeDoctor(CompleteDoctorRequest request) {
		DoctorInvite invite = requireUsableInvite(request.token());
		if (userRepository.existsByEmailIgnoreCase(invite.getEmail())) {
			throw new ConflictException("E-mail já cadastrado");
		}
		String crm = request.crm().trim().toUpperCase(Locale.ROOT);
		if (doctorProfileRepository.existsByCrm(crm)) {
			throw new ConflictException("CRM já cadastrado");
		}
		User user = User.create(invite.getEmail(), passwordEncoder.encode(request.password()), Role.MEDICO);
		userRepository.saveAndFlush(user);
		DoctorProfile profile = DoctorProfile.of(user, invite.getFullName(), crm, request.specialty().trim());
		doctorProfileRepository.save(profile);
		user.attachDoctorProfile(profile);
		invite.accept();
		return new TokenResponse(jwtService.issueToken(user));
	}

	@Transactional(readOnly = true)
	public List<DoctorInviteResponse> listInvites() {
		Instant now = Instant.now();
		return doctorInviteRepository.findAllByOrderByCreatedAtDesc().stream()
				.map(invite -> toInviteResponse(invite, invite.isAccepted() || invite.isExpired(now) ? null : doctorInviteUrl(invite.getToken())))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<ManagedDoctorResponse> listDoctors() {
		return doctorProfileRepository.findAllWithUserOrderByFullNameAsc().stream()
				.map(this::toManaged)
				.toList();
	}

	@Transactional
	public ManagedDoctorResponse setDoctorEnabled(UUID doctorId, boolean enabled) {
		User user = userRepository.findById(doctorId)
				.orElseThrow(() -> new NotFoundException("Médico não encontrado"));
		if (user.getRole() != Role.MEDICO) {
			throw new BusinessException("Somente contas de médico podem ser ativadas ou desativadas por aqui");
		}
		user.setEnabled(enabled);
		DoctorProfile profile = doctorProfileRepository.findByUserId(doctorId)
				.orElseThrow(() -> new NotFoundException("Perfil de médico não encontrado"));
		return new ManagedDoctorResponse(
				profile.getUserId(),
				user.getEmail(),
				profile.getFullName(),
				profile.getCrm(),
				profile.getSpecialty(),
				user.isEnabled());
	}

	private ManagedDoctorResponse toManaged(DoctorProfile profile) {
		User user = profile.getUser();
		return new ManagedDoctorResponse(
				profile.getUserId(),
				user == null ? null : user.getEmail(),
				profile.getFullName(),
				profile.getCrm(),
				profile.getSpecialty(),
				user == null || user.isEnabled());
	}

	private UUID consumeAndRotate(UUID presented) {
		AdminBootstrapToken current = bootstrapTokenRepository.lockCurrent()
				.orElseThrow(() -> new BusinessException("Token de cadastro de admin inválido"));
		if (!current.getToken().equals(presented)) {
			throw new BusinessException("Token de cadastro de admin inválido");
		}
		bootstrapTokenRepository.delete(current);
		bootstrapTokenRepository.flush();
		UUID next = UUID.randomUUID();
		bootstrapTokenRepository.save(AdminBootstrapToken.of(next));
		return next;
	}

	private DoctorInvite requireUsableInvite(UUID token) {
		DoctorInvite invite = doctorInviteRepository.findByToken(token)
				.orElseThrow(() -> new NotFoundException("Convite não encontrado"));
		if (invite.isAccepted()) {
			throw new BusinessException("Este convite já foi utilizado");
		}
		if (invite.isExpired(Instant.now())) {
			throw new BusinessException("Este convite expirou. Peça um novo ao administrador");
		}
		return invite;
	}

	private String doctorInviteUrl(UUID token) {
		String base = mailProperties.frontendBaseUrl().replaceAll("/$", "");
		return base + "/cadastro/medico?token=" + token;
	}

	private DoctorInviteResponse toInviteResponse(DoctorInvite invite, String inviteUrl) {
		Instant now = Instant.now();
		String status;
		if (invite.isAccepted()) {
			status = "ACCEPTED";
		} else if (invite.isExpired(now)) {
			status = "EXPIRED";
		} else {
			status = "PENDING";
		}
		UUID token = "PENDING".equals(status) ? invite.getToken() : null;
		return new DoctorInviteResponse(
				invite.getId(),
				invite.getEmail(),
				invite.getFullName(),
				token,
				inviteUrl,
				status,
				invite.getExpiresAt(),
				invite.getAcceptedAt(),
				invite.getCreatedAt());
	}
}
