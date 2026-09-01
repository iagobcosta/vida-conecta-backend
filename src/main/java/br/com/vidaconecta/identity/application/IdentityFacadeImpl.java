package br.com.vidaconecta.identity.application;

import br.com.vidaconecta.identity.api.IdentityFacade;
import br.com.vidaconecta.identity.api.Role;
import br.com.vidaconecta.identity.domain.DoctorProfile;
import br.com.vidaconecta.identity.infrastructure.AdminProfileRepository;
import br.com.vidaconecta.identity.infrastructure.DoctorInviteRepository;
import br.com.vidaconecta.identity.infrastructure.DoctorProfileRepository;
import br.com.vidaconecta.identity.infrastructure.PatientProfileRepository;
import br.com.vidaconecta.identity.infrastructure.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class IdentityFacadeImpl implements IdentityFacade {

	private final UserRepository userRepository;
	private final DoctorProfileRepository doctorProfileRepository;
	private final PatientProfileRepository patientProfileRepository;
	private final AdminProfileRepository adminProfileRepository;
	private final DoctorInviteRepository doctorInviteRepository;

	public IdentityFacadeImpl(
			UserRepository userRepository,
			DoctorProfileRepository doctorProfileRepository,
			PatientProfileRepository patientProfileRepository,
			AdminProfileRepository adminProfileRepository,
			DoctorInviteRepository doctorInviteRepository) {
		this.userRepository = userRepository;
		this.doctorProfileRepository = doctorProfileRepository;
		this.patientProfileRepository = patientProfileRepository;
		this.adminProfileRepository = adminProfileRepository;
		this.doctorInviteRepository = doctorInviteRepository;
	}

	@Override
	public boolean existsUser(UUID userId) {
		return userRepository.existsById(userId);
	}

	@Override
	public boolean isDoctor(UUID userId) {
		return userRepository.findById(userId).filter(user -> user.getRole() == Role.MEDICO).isPresent();
	}

	@Override
	public boolean isActiveDoctor(UUID userId) {
		return userRepository.findById(userId)
				.filter(user -> user.getRole() == Role.MEDICO && user.isEnabled())
				.isPresent();
	}

	@Override
	public boolean isPatient(UUID userId) {
		return userRepository.findById(userId).filter(user -> user.getRole() == Role.PACIENTE).isPresent();
	}

	@Override
	public Optional<DoctorView> findDoctor(UUID userId) {
		return doctorProfileRepository.findByUserId(userId).map(this::toView);
	}

	@Override
	public Optional<PatientView> findPatient(UUID userId) {
		return patientProfileRepository.findByUserId(userId)
				.map(profile -> new PatientView(profile.getUserId(), profile.getFullName(), profile.getCpf()));
	}

	@Override
	public List<DoctorView> listDoctors() {
		return listAllDoctors().stream().filter(DoctorView::enabled).toList();
	}

	@Override
	public List<DoctorView> listAllDoctors() {
		return doctorProfileRepository.findAllWithUserOrderByFullNameAsc().stream()
				.map(this::toView)
				.toList();
	}

	@Override
	public SystemCensus census() {
		long doctorsActive = userRepository.countByRoleAndEnabled(Role.MEDICO, true);
		long doctorsTotal = userRepository.countByRole(Role.MEDICO);
		return new SystemCensus(
				userRepository.countByRole(Role.PACIENTE),
				doctorsActive,
				Math.max(0, doctorsTotal - doctorsActive),
				userRepository.countByRole(Role.ADMIN),
				doctorInviteRepository.countByAcceptedAtIsNullAndExpiresAtAfter(Instant.now()));
	}

	@Override
	public Optional<AdminView> findAdmin(UUID userId) {
		return adminProfileRepository.findByUserId(userId)
				.map(profile -> new AdminView(profile.getUserId(), profile.getFullName()));
	}

	private DoctorView toView(DoctorProfile profile) {
		boolean enabled = profile.getUser() == null || profile.getUser().isEnabled();
		return new DoctorView(profile.getUserId(), profile.getFullName(), profile.getCrm(), profile.getSpecialty(), enabled);
	}
}
