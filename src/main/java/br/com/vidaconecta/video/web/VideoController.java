package br.com.vidaconecta.video.web;

import br.com.vidaconecta.identity.api.CurrentUser;
import br.com.vidaconecta.video.application.VideoService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/video")
public class VideoController {

	private final VideoService videoService;

	public VideoController(VideoService videoService) {
		this.videoService = videoService;
	}

	@PostMapping("/appointments/{id}/token")
	public VideoTokenResponse token(
			@AuthenticationPrincipal CurrentUser currentUser,
			@PathVariable UUID id) {
		return videoService.issueToken(currentUser, id);
	}

	@PostMapping("/appointments/{id}/session")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void session(
			@AuthenticationPrincipal CurrentUser currentUser,
			@PathVariable UUID id,
			@Valid @RequestBody VideoSessionEventRequest request) {
		videoService.recordSession(currentUser, id, request.event());
	}
}
