package br.com.vidaconecta.portability.application;

import java.util.UUID;

import org.springframework.stereotype.Service;

import br.com.vidaconecta.ehr.api.EhrFacade;
import br.com.vidaconecta.identity.api.CurrentUser;
import br.com.vidaconecta.identity.api.IdentityFacade;
import br.com.vidaconecta.identity.api.IdentityFacade.PatientExportView;
import br.com.vidaconecta.portability.web.DataExportResponse;
import br.com.vidaconecta.prescription.api.PrescriptionFacade;

@Service
public class PortabilityService {

	private final IdentityFacade identityFacade;
	private final EhrFacade ehrFacade;
	private final PrescriptionFacade prescriptionFacade;

	public PortabilityService(IdentityFacade identityFacade, EhrFacade ehrFacade, PrescriptionFacade prescriptionFacade) {
		this.identityFacade = identityFacade;
		this.ehrFacade = ehrFacade;
		this.prescriptionFacade = prescriptionFacade;
	}

	public DataExportResponse exportPatientData(CurrentUser currentUser) {

		PatientExportView profile = identityFacade.exportPatient(currentUser.id())
				.orElseThrow(() -> new IllegalArgumentException("Paciente não encontrado"));

		UUID patientId = currentUser.id();

		var notes = ehrFacade.exportPatientNotes(currentUser, patientId);
		var audit = ehrFacade.exportPatientAudit(currentUser, patientId);
		var prescriptions = prescriptionFacade.exportPatientPrescriptions(patientId);

		return new DataExportResponse(
				profile,
				null, // appointments
				null, // consents
				notes,
				prescriptions,
				audit
		);
	}
}
