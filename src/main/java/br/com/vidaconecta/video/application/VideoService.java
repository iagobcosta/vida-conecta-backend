package br.com.vidaconecta.video.application;

import br.com.vidaconecta.identity.api.CurrentUser;
import br.com.vidaconecta.identity.api.IdentityFacade;
import br.com.vidaconecta.scheduling.api.SchedulingFacade;
import br.com.vidaconecta.shared.api.ForbiddenException;
import br.com.vidaconecta.shared.api.NotFoundException;
import br.com.vidaconecta.video.api.VideoRoomProvider;
import br.com.vidaconecta.video.api.VideoSessionEvent;
import br.com.vidaconecta.video.web.VideoTokenResponse;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class VideoService {

	private final SchedulingFacade schedulingFacade;
	private final VideoRoomProvider videoRoomProvider;
	private final IdentityFacade identityFacade;
	private final VideoCallMetrics videoCallMetrics;

	public VideoService(
			SchedulingFacade schedulingFacade,
			VideoRoomProvider videoRoomProvider,
			IdentityFacade identityFacade,
			VideoCallMetrics videoCallMetrics) {
		this.schedulingFacade = schedulingFacade;
		this.videoRoomProvider = videoRoomProvider;
		this.identityFacade = identityFacade;
		this.videoCallMetrics = videoCallMetrics;
	}

	public VideoTokenResponse issueToken(CurrentUser currentUser, UUID appointmentId) {
		schedulingFacade.findById(appointmentId)
				.orElseThrow(() -> new NotFoundException("Consulta não encontrada"));
		if (!schedulingFacade.canJoinVideo(appointmentId, currentUser.id(), Instant.now())) {
			throw new ForbiddenException("Você não pode entrar nesta sala agora");
		}
		String displayName = resolveDisplayName(currentUser);
		var token = videoRoomProvider.issueToken(appointmentId, currentUser.id(), displayName);
		videoCallMetrics.recordTokenIssued();
		return new VideoTokenResponse(token.roomName(), token.token(), token.url());
	}

	public void recordSession(CurrentUser currentUser, UUID appointmentId, VideoSessionEvent event) {
		schedulingFacade.findById(appointmentId)
				.orElseThrow(() -> new NotFoundException("Consulta não encontrada"));
		if (!schedulingFacade.isParticipant(appointmentId, currentUser.id())) {
			throw new ForbiddenException("Você não participa desta consulta");
		}
		videoCallMetrics.record(event);
	}

	private String resolveDisplayName(CurrentUser currentUser) {
		if (currentUser.isDoctor()) {
			return identityFacade.findDoctor(currentUser.id())
					.map(IdentityFacade.DoctorView::fullName)
					.orElse(currentUser.email());
		}
		if (currentUser.isPatient()) {
			return identityFacade.findPatient(currentUser.id())
					.map(IdentityFacade.PatientView::fullName)
					.orElse(currentUser.email());
		}
		return currentUser.email();
	}
}
