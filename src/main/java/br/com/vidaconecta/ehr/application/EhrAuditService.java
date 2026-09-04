package br.com.vidaconecta.ehr.application;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.vidaconecta.ehr.domain.EhrAccessAudit;
import br.com.vidaconecta.ehr.infrastructure.EhrAccessAuditRepository;

@Service
public class EhrAuditService {
    private final EhrAccessAuditRepository auditRepository;
    public EhrAuditService(EhrAccessAuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UUID actorUserId, UUID patientId, UUID appointmentId, String action) {
        auditRepository.save(EhrAccessAudit.record(actorUserId, patientId, appointmentId, action));
    }
}
