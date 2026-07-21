package com.hanzware.lumora.starter;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import java.util.HashMap;
import java.util.Map;

/**
 * Logback appender — captures every log event and routes it to the ring buffer.
 * Add to logback-spring.xml:
 *
 * <appender name="LUMORA" class="com.hanzware.lumora.starter.LumoraLogAppender">
 *     <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
 *         <level>INFO</level>
 *     </filter>
 * </appender>
 * <root level="INFO"><appender-ref ref="LUMORA"/></root>
 *
 * OR let LumoraAutoConfiguration register it programmatically.
 */
public class LumoraLogAppender extends AppenderBase<ILoggingEvent> {

    private RingBufferQueue queue;

    public void setQueue(RingBufferQueue queue) {
        this.queue = queue;
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (queue == null) return;
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("level", event.getLevel().toString());
            payload.put("message", event.getFormattedMessage());
            payload.put("logger", event.getLoggerName());
            payload.put("thread", event.getThreadName());

            // MDC values for distributed tracing
            Map<String, String> mdc = event.getMDCPropertyMap();
            if (mdc != null && !mdc.isEmpty()) {
                payload.put("traceId", mdc.get("traceId"));
                payload.put("userId", mdc.get("userId"));
                payload.put("tenantId", mdc.get("tenantId"));
                
                // Store other MDC fields as contextData
                Map<String, String> contextData = new HashMap<>(mdc);
                contextData.remove("traceId");
                contextData.remove("userId");
                contextData.remove("tenantId");
                if (!contextData.isEmpty()) {
                    payload.put("contextData", contextData);
                }
            }
            
            // Capture Exception stack trace
            if (event.getThrowableProxy() != null) {
                payload.put("exception", ch.qos.logback.classic.spi.ThrowableProxyUtil.asString(event.getThrowableProxy()));
            }

            queue.offer("LOG", payload);
        } catch (Exception e) {
            // Never crash the user's app
            addWarn("Lumora appender error: " + e.getMessage());
        }
    }
}
