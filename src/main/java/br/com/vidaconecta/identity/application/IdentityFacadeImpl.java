package br.com.vidaconecta.identity.application;

import br.com.vidaconecta.identity.api.IdentityFacade;
import br.com.vidaconecta.identity.api.Role;
import br.com.vidaconecta.identity.infrastructure.AdminProfileRepository;
import br.com.vidaconecta.identity.infrastructure.DoctorProfileRepository;
import br.com.vidaconecta.identity.infrastructure.PatientProfileRepository;
import br.com.vidaconecta.identity.infrastructure.UserRepository;
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

	public IdentityFacadeImpl(
			UserRepository userRepository,
			DoctorProfileRepository doctorProfileRepository,
			PatientProfileRepository patientProfileRepository,
			AdminProfileRepository adminProfileRepository) {
		this.userRepository = userRepository;
		this.doctorProfileRepository = doctorProfileRepository;
		this.patientProfileRepository = patientProfileRepository;
		this.adminProfileRepository = adminProfileRepository;
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
	public boolean isPatient(UUID userId) {
		return userRepository.findById(userId).filter(user -> user.getRole() == Role.PACIENTE).isPresent();
	}

	@Override
	public Optional<DoctorView> findDoctor(UUID userId) {
		return doctorProfileRepository.findByUserId(userId)
				.map(profile -> new DoctorView(profile.getUserId(), profile.getFullName(), profile.getCrm(), profile.getSpecialty()));
	}

	@Override
	public Optional<PatientView> findPatient(UUID userId) {
		return patientProfileRepository.findByUserId(userId)
				.map(profile -> new PatientView(profile.getUserId(), profile.getFullName(), profile.getCpf()));
	}

	@Override
	public List<DoctorView> listDoctors() {
		return doctorProfileRepository.findAllByOrderByFullNameAsc().stream()
				.map(profile -> new DoctorView(profile.getUserId(), profile.getFullName(), profile.getCrm(), profile.getSpecialty()))
				.toList();
	}

	@Override
	public Optional<AdminView> findAdmin(UUID userId) {
		return adminProfileRepository.findByUserId(userId)
				.map(profile -> new AdminView(profile.getUserId(), profile.getFullName()));
	}
}
