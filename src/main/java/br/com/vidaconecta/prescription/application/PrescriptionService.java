package br.com.vidaconecta.prescription.application;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.vidaconecta.identity.api.CurrentUser;
import br.com.vidaconecta.identity.api.IdentityFacade;
import br.com.vidaconecta.notification.api.NotificationFacade;
import br.com.vidaconecta.notification.api.NotificationType;
import br.com.vidaconecta.prescription.domain.Prescription;
import br.com.vidaconecta.prescription.domain.PrescriptionItem;
import br.com.vidaconecta.prescription.infrastructure.PrescriptionRepository;
import br.com.vidaconecta.prescription.web.CreatePrescriptionRequest;
import br.com.vidaconecta.prescription.web.PrescriptionItemResponse;
import br.com.vidaconecta.prescription.web.PrescriptionResponse;
import br.com.vidaconecta.scheduling.api.SchedulingFacade;
import br.com.vidaconecta.shared.api.ForbiddenException;
import br.com.vidaconecta.shared.api.NotFoundException;

@Service
public class PrescriptionService {

	private final PrescriptionRepository prescriptionRepository;
	private final SchedulingFacade schedulingFacade;
	private final IdentityFacade identityFacade;
	private final NotificationFacade notificationFacade;

	public PrescriptionService(
			PrescriptionRepository prescriptionRepository,
			SchedulingFacade schedulingFacade,
			IdentityFacade identityFacade,
			NotificationFacade notificationFacade) {
		this.prescriptionRepository = prescriptionRepository;
		this.schedulingFacade = schedulingFacade;
		this.identityFacade = identityFacade;
		this.notificationFacade = notificationFacade;
	}

	@Transactional
	public PrescriptionResponse create(CurrentUser currentUser, CreatePrescriptionRequest request) {
		if (!currentUser.isDoctor()) {
			throw new ForbiddenException("Somente médicos podem emitir prescrição");
		}
		SchedulingFacade.AppointmentView appointment = schedulingFacade.findById(request.appointmentId())
				.orElseThrow(() -> new NotFoundException("Consulta não encontrada"));
		if (!appointment.doctorId().equals(currentUser.id())) {
			throw new ForbiddenException("Somente o médico da consulta pode emitir a receita");
		}
		if (!appointment.patientId().equals(request.patientId())) {
			throw new ForbiddenException("Paciente não corresponde à consulta");
		}
		List<PrescriptionItem> items = request.items().stream()
				.map(item -> PrescriptionItem.of(item.medication(), item.dosage(), item.instructions()))
				.toList();
		Prescription prescription = Prescription.issue(
				request.patientId(),
				currentUser.id(),
				request.appointmentId(),
				items);
		prescriptionRepository.save(prescription);
		notificationFacade.push(new NotificationFacade.NewNotification(
				request.patientId(),
				NotificationType.PRESCRIPTION_ISSUED,
				"Nova receita disponível",
				identityFacade.displayName(currentUser.id()) + " emitiu uma receita da sua consulta.",
				request.appointmentId(),
				"/receitas",
				"Ver receita"));
		return toResponse(prescription);
	}

	@Transactional(readOnly = true)
	public List<PrescriptionResponse> list(CurrentUser currentUser) {
		List<Prescription> prescriptions = currentUser.isDoctor()
				? prescriptionRepository.findByDoctorIdOrderByIssuedAtDesc(currentUser.id())
				: prescriptionRepository.findByPatientIdOrderByIssuedAtDesc(currentUser.id());
		return prescriptions.stream().map(this::toResponse).toList();
	}

	@Transactional(readOnly = true)
	public PrescriptionResponse get(CurrentUser currentUser, UUID id) {
		Prescription prescription = prescriptionRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Prescrição não encontrada"));
		if (!prescription.isVisibleTo(currentUser.id()) && !currentUser.isAdmin()) {
			throw new ForbiddenException("Você não pode ver esta prescrição");
		}
		return toResponse(prescription);
	}

	private PrescriptionResponse toResponse(Prescription prescription) {
		String doctorName = identityFacade.findDoctor(prescription.getDoctorId())
				.map(IdentityFacade.DoctorView::fullName)
				.orElse(null);
		List<PrescriptionItemResponse> items = prescription.getItems().stream()
				.map(item -> new PrescriptionItemResponse(item.getMedication(), item.getDosage(), item.getInstructions()))
				.toList();
		return new PrescriptionResponse(
				prescription.getId(),
				prescription.getPatientId(),
				prescription.getDoctorId(),
				doctorName,
				prescription.getAppointmentId(),
				prescription.getIssuedAt(),
				items);
	}

	@Transactional(readOnly = true)
	public List<PrescriptionResponse> listByPatient(UUID patientId) {
		return prescriptionRepository.findByPatientIdOrderByIssuedAtDesc(patientId)
				.stream()
				.map(this::toResponse)
				.toList();
	}
}
