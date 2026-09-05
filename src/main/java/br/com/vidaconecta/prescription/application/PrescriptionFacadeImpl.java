package br.com.vidaconecta.prescription.application;

import br.com.vidaconecta.prescription.api.PrescriptionFacade;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PrescriptionFacadeImpl implements PrescriptionFacade {
    
    private final PrescriptionService prescriptionService;

    public PrescriptionFacadeImpl(PrescriptionService prescriptionService) {
        this.prescriptionService = prescriptionService;
    }

    @Override
    public List<?> exportPatientPrescriptions(UUID patientId) {
        return prescriptionService.listByPatient(patientId);
    }
}
