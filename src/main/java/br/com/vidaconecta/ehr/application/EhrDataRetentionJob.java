package br.com.vidaconecta.ehr.application;

import br.com.vidaconecta.ehr.infrastructure.ClinicalNoteRepository;
import br.com.vidaconecta.ehr.infrastructure.EhrAccessAuditRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class EhrDataRetentionJob {
    private static final Logger log = LoggerFactory.getLogger(EhrDataRetentionJob.class);

    private static final int AUDIT_RETENTION_YEARS = 5;
    private static final int CLINICAL_RETENTION_YEARS = 20;

    private final EhrAccessAuditRepository auditRepository;
    private final ClinicalNoteRepository clinicalNoteRepository;

    public EhrDataRetentionJob(EhrAccessAuditRepository auditRepository,
            ClinicalNoteRepository clinicalNoteRepository) {
        this.auditRepository = auditRepository;
        this.clinicalNoteRepository = clinicalNoteRepository;
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupOldAuditLogs() {
        log.info("Iniciando rotina LGPD/CFM de retenção de dados...");

        Instant auditCutoff = Instant.now().minus(AUDIT_RETENTION_YEARS * 365L, ChronoUnit.DAYS);
        int deletedAudits = auditRepository.deleteByAccessedAtBefore(auditCutoff);

        Instant clinicalCutoff = Instant.now().minus(CLINICAL_RETENTION_YEARS * 365L, ChronoUnit.DAYS);
        int deletedNotes = clinicalNoteRepository.deleteByCreatedAtBefore(clinicalCutoff);

        log.info("Rotina finalizada. Apagados: {} logs de auditoria (>5 anos) e {} registros clínicos (>20 anos).",
                deletedAudits, deletedNotes);
    }
}
