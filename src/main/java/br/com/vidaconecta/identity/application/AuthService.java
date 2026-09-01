package br.com.vidaconecta.identity.application;

import br.com.vidaconecta.identity.api.CurrentUser;
import br.com.vidaconecta.identity.api.Role;
import br.com.vidaconecta.identity.domain.AdminProfile;
import br.com.vidaconecta.identity.domain.DoctorProfile;
import br.com.vidaconecta.identity.domain.PatientProfile;
import br.com.vidaconecta.identity.domain.User;
import br.com.vidaconecta.identity.infrastructure.AdminProfileRepository;
import br.com.vidaconecta.identity.infrastructure.DoctorProfileRepository;
import br.com.vidaconecta.identity.infrastructure.PatientProfileRepository;
import br.com.vidaconecta.identity.infrastructure.UserRepository;
import br.com.vidaconecta.identity.web.LoginRequest;
import br.com.vidaconecta.identity.web.MeResponse;
import br.com.vidaconecta.identity.web.RegisterRequest;
import br.com.vidaconecta.identity.web.TokenResponse;
import br.com.vidaconecta.shared.api.BusinessException;
import br.com.vidaconecta.shared.api.ConflictException;
import br.com.vidaconecta.shared.api.NotFoundException;
import java.util.Locale;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final PatientProfileRepository patientProfileRepository;
	private final DoctorProfileRepository doctorProfileRepository;
	private final AdminProfileRepository adminProfileRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	public AuthService(
			UserRepository userRepository,
			PatientProfileRepository patientProfileRepository,
			DoctorProfileRepository doctorProfileRepository,
			AdminProfileRepository adminProfileRepository,
			PasswordEncoder passwordEncoder,
			JwtService jwtService) {
		this.userRepository = userRepository;
		this.patientProfileRepository = patientProfileRepository;
		this.doctorProfileRepository = doctorProfileRepository;
		this.adminProfileRepository = adminProfileRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
	}

	@Transactional
	public TokenResponse register(RegisterRequest request) {
		if (request.role() != Role.PACIENTE) {
			throw new BusinessException("O cadastro público é exclusivo para pacientes. Médicos entram por convite do administrador");
		}
		String email = request.email().trim().toLowerCase(Locale.ROOT);
		if (userRepository.existsByEmailIgnoreCase(email)) {
			throw new ConflictException("E-mail já cadastrado");
		}
		User user = User.create(email, passwordEncoder.encode(request.password()), Role.PACIENTE);
		userRepository.saveAndFlush(user);
		registerPatient(user, request);
		return new TokenResponse(jwtService.issueToken(user));
	}

	@Transactional
	public TokenResponse login(LoginRequest request) {
		String email = request.email().trim().toLowerCase(Locale.ROOT);
		User user = userRepository.findByEmailIgnoreCase(email)
				.filter(User::isEnabled)
				.orElseThrow(() -> new BadCredentialsException("Credenciais inválidas"));
		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw new BadCredentialsException("Credenciais inválidas");
		}
		return new TokenResponse(jwtService.issueToken(user));
	}

	@Transactional
	public MeResponse me(CurrentUser currentUser) {
		User user = userRepository.findById(currentUser.id())
				.orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
		if (user.getRole() == Role.PACIENTE) {
			PatientProfile profile = patientProfileRepository.findByUserId(user.getId())
					.orElseThrow(() -> new NotFoundException("Perfil de paciente não encontrado"));
			return MeResponse.patient(user, profile);
		}
		if (user.getRole() == Role.MEDICO) {
			DoctorProfile profile = doctorProfileRepository.findByUserId(user.getId())
					.orElseThrow(() -> new NotFoundException("Perfil de médico não encontrado"));
			return MeResponse.doctor(user, profile);
		}
		AdminProfile profile = adminProfileRepository.findByUserId(user.getId()).orElse(null);
		return MeResponse.admin(user, profile);
	}

	private void registerPatient(User user, RegisterRequest request) {
		if (isBlank(request.cpf()) || request.birthDate() == null) {
			throw new BusinessException("Paciente precisa informar CPF e data de nascimento");
		}
		String cpf = request.cpf().replaceAll("\\D", "");
		if (cpf.length() != 11) {
			throw new BusinessException("CPF deve conter 11 dígitos");
		}
		if (patientProfileRepository.existsByCpf(cpf)) {
			throw new ConflictException("CPF já cadastrado");
		}
		PatientProfile profile = PatientProfile.of(user, request.fullName().trim(), cpf, request.birthDate(), request.phone());
		patientProfileRepository.save(profile);
		user.attachPatientProfile(profile);
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
