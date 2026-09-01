package br.com.vidaconecta.identity.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IdentityFacade {

	boolean existsUser(UUID userId);

	boolean isDoctor(UUID userId);

	boolean isPatient(UUID userId);

	Optional<DoctorView> findDoctor(UUID userId);

	Optional<PatientView> findPatient(UUID userId);

	List<DoctorView> listDoctors();

	List<DoctorView> listAllDoctors();

	boolean isActiveDoctor(UUID userId);

	SystemCensus census();

	Optional<AdminView> findAdmin(UUID userId);

	default String displayName(UUID userId) {
		return findDoctor(userId)
				.map(DoctorView::fullName)
				.or(() -> findPatient(userId).map(PatientView::fullName))
				.or(() -> findAdmin(userId).map(AdminView::fullName))
				.orElse("Usuário");
	}

	record DoctorView(UUID userId, String fullName, String crm, String specialty, boolean enabled) {
	}

	record SystemCensus(
			long patients,
			long doctorsActive,
			long doctorsInactive,
			long admins,
			long pendingInvites) {
	}

	record PatientView(UUID userId, String fullName, String cpf) {
	}

	record AdminView(UUID userId, String fullName) {
	}
}
