package br.com.vidaconecta.prescription.api;

import java.util.List;
import java.util.UUID;

public interface PrescriptionFacade {
    List<?> exportPatientPrescriptions(UUID patientId);
}
