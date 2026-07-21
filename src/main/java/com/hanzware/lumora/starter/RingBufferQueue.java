package com.hanzware.lumora.starter;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Ring buffer queue — holds telemetry events until flush.
 * If full, drops oldest to protect app memory.
 * Background thread flushes every flushIntervalMs or when batchSize reached.
 */
public class RingBufferQueue {

    private final BlockingQueue<Map<String, Object>> queue;
    private final TelemetryClient client;
    private final LumoraProperties props;
    private final ScheduledExecutorService scheduler;

    public RingBufferQueue(TelemetryClient client, LumoraProperties props) {
        this.client = client;
        this.props = props;
        this.queue = new ArrayBlockingQueue<>(props.getQueueCapacity());
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "lumora-flush");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::flush, props.getFlushIntervalMs(),
                props.getFlushIntervalMs(), TimeUnit.MILLISECONDS);
    }

    public void offer(String type, Map<String, Object> payload) {
        Map<String, Object> envelope = new java.util.HashMap<>();
        envelope.put("projectId", props.getProjectId());
        envelope.put("type", type);
        envelope.put("occurredAt", Instant.now().toString());
        envelope.put("payload", payload);

        if (!queue.offer(envelope)) {
            // Queue full — drop oldest, add new
            queue.poll();
            queue.offer(envelope);
        }

        if (queue.size() >= props.getBatchSize()) {
            scheduler.submit(this::flush);
        }
    }

    private synchronized void flush() {
        if (queue.isEmpty()) return;
        List<Map<String, Object>> batch = new ArrayList<>(props.getBatchSize());
        queue.drainTo(batch, props.getBatchSize());
        if (!batch.isEmpty()) client.sendBatch(batch);
    }

    public void shutdown() {
        scheduler.shutdown();
        flush(); // final flush
    }
}
