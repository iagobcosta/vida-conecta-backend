package br.com.vidaconecta.video.application;

import br.com.vidaconecta.video.api.VideoSessionEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class VideoCallMetrics {

	private final Counter tokensIssued;
	private final Map<VideoSessionEvent, Counter> sessionEvents = new EnumMap<>(VideoSessionEvent.class);

	public VideoCallMetrics(MeterRegistry meterRegistry) {
		this.tokensIssued = sessionCounter(meterRegistry, "issued");
		for (VideoSessionEvent event : VideoSessionEvent.values()) {
			sessionEvents.put(event, sessionCounter(meterRegistry, event.name().toLowerCase()));
		}
	}

	public void recordTokenIssued() {
		tokensIssued.increment();
	}

	public void record(VideoSessionEvent event) {
		sessionEvents.get(event).increment();
	}

	private static Counter sessionCounter(MeterRegistry meterRegistry, String event) {
		return Counter.builder("vida_conecta_video_sessions")
				.tag("event", event)
				.description("Sessões de videochamada (token, entrada e encerramento)")
				.register(meterRegistry);
	}
}