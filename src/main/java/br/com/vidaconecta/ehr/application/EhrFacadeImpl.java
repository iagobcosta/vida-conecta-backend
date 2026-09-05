package br.com.vidaconecta.ehr.application;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import br.com.vidaconecta.ehr.api.EhrFacade;
import br.com.vidaconecta.identity.api.CurrentUser;

@Service
public class EhrFacadeImpl implements EhrFacade {
    
    private final EhrService ehrService;

    public EhrFacadeImpl(EhrService ehrService) {
        this.ehrService = ehrService;
    }

    @Override
    public List<?> exportPatientNotes(CurrentUser currentUser, UUID patientId) {
        return ehrService.list(currentUser, patientId, null); 
    }

    @Override
    public List<?> exportPatientAudit(CurrentUser currentUser, UUID patientId) {
        return ehrService.listAudit(currentUser, patientId);
    }
}
