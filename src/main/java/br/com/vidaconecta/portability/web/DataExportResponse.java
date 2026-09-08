package br.com.vidaconecta.portability.web;

import br.com.vidaconecta.identity.api.IdentityFacade.PatientExportView;

import java.util.List;

public record DataExportResponse(

    PatientExportView profile,
    List<?> appointments,
    List<?> consents,
    List<?> clinicalNotes,
    List<?> prescriptions,
    List<?> auditLogs

) {}
