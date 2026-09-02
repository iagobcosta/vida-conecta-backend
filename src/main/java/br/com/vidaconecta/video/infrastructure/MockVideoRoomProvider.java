package br.com.vidaconecta.video.infrastructure;

import br.com.vidaconecta.video.api.VideoRoomProvider;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class MockVideoRoomProvider implements VideoRoomProvider {

	@Override
	public RoomToken issueToken(UUID appointmentId, UUID userId, String displayName) {
		String roomName = "appointment-" + appointmentId;
		String token = "mock-" + userId + "-" + appointmentId;
		return new RoomToken(roomName, token, "wss://mock.local");
	}
}
