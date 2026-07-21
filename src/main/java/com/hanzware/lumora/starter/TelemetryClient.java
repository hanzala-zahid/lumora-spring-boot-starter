package com.hanzware.lumora.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Sends telemetry batches to Lumora ingest endpoint.
 * Uses Java 11 built-in HttpClient — zero extra dependencies.
 * Never throws — failures are logged and discarded to protect the user's app.
 */
public class TelemetryClient {

    private static final Logger log = Logger.getLogger(TelemetryClient.class.getName());

    private final LumoraProperties props;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    public TelemetryClient(LumoraProperties props) {
        this.props = props;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(props.getHttpTimeoutSeconds()))
                .build();
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public void sendBatch(List<Map<String, Object>> events) {
        if (events.isEmpty()) return;
        int attempt = 0;
        while (attempt < 3) {
            try {
                Map<String, Object> body = Map.of("events", events);
                String json = mapper.writeValueAsString(body);

                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(props.getIngestUrl()))
                        .header("Content-Type", "application/json")
                        .header("X-Lumora-Key", props.getApiKey())
                        .header("User-Agent", "LumoraAgent/1.0")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .timeout(Duration.ofSeconds(props.getHttpTimeoutSeconds()))
                        .build();

                HttpResponse<Void> resp = httpClient.send(req, HttpResponse.BodyHandlers.discarding());
                if (resp.statusCode() == 202) return;

                log.warning("Lumora ingest returned HTTP " + resp.statusCode() + " — attempt " + (attempt + 1));
            } catch (Exception e) {
                log.warning("Lumora ingest failed (attempt " + (attempt + 1) + "): " + e.getMessage());
            }
            attempt++;
            try { Thread.sleep(1000L * attempt); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); return; }
        }
        log.warning("Lumora: batch discarded after 3 failed attempts (" + events.size() + " events)");
    }
}
