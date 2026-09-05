package br.com.vidaconecta.ehr.api;

import java.util.List;
import java.util.UUID;
import br.com.vidaconecta.identity.api.CurrentUser;

public interface EhrFacade {

    List<?> exportPatientNotes(CurrentUser currentUser, UUID patientId);
    List<?> exportPatientAudit(CurrentUser currentUser, UUID patientId);
    
}