package br.com.vidaconecta.identity.application;

import br.com.vidaconecta.identity.api.CurrentUser;
import br.com.vidaconecta.identity.api.Role;
import br.com.vidaconecta.identity.domain.DoctorProfile;
import br.com.vidaconecta.identity.domain.PatientProfile;
import br.com.vidaconecta.identity.domain.User;
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
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final PatientProfileRepository patientProfileRepository;
	private final DoctorProfileRepository doctorProfileRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	public AuthService(
			UserRepository userRepository,
			PatientProfileRepository patientProfileRepository,
			DoctorProfileRepository doctorProfileRepository,
			PasswordEncoder passwordEncoder,
			JwtService jwtService) {
		this.userRepository = userRepository;
		this.patientProfileRepository = patientProfileRepository;
		this.doctorProfileRepository = doctorProfileRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
	}

	@Transactional
	public TokenResponse register(RegisterRequest request) {
		if (request.role() == Role.ADMIN) {
			throw new BusinessException("Cadastro de administrador não é permitido");
		}
		String email = request.email().trim().toLowerCase(Locale.ROOT);
		if (userRepository.existsByEmailIgnoreCase(email)) {
			throw new ConflictException("E-mail já cadastrado");
		}
		User user = User.create(email, passwordEncoder.encode(request.password()), request.role());
		userRepository.saveAndFlush(user);
		if (request.role() == Role.PACIENTE) {
			registerPatient(user, request);
		} else if (request.role() == Role.MEDICO) {
			registerDoctor(user, request);
		}
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
		return MeResponse.admin(user);
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

	private void registerDoctor(User user, RegisterRequest request) {
		if (isBlank(request.crm()) || isBlank(request.specialty())) {
			throw new BusinessException("Médico precisa informar CRM e especialidade");
		}
		String crm = request.crm().trim().toUpperCase(Locale.ROOT);
		if (doctorProfileRepository.existsByCrm(crm)) {
			throw new ConflictException("CRM já cadastrado");
		}
		DoctorProfile profile = DoctorProfile.of(user, request.fullName().trim(), crm, request.specialty().trim());
		doctorProfileRepository.save(profile);
		user.attachDoctorProfile(profile);
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
