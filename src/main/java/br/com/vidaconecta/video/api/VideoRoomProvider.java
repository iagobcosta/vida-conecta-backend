package br.com.vidaconecta.video.api;

import java.util.UUID;

public interface VideoRoomProvider {

	RoomToken issueToken(UUID appointmentId, UUID userId, String displayName);

	record RoomToken(String roomName, String token, String url) {
	}
}
