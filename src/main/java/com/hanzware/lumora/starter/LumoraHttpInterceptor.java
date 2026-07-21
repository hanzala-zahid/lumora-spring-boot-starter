package com.hanzware.lumora.starter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.HashMap;
import java.util.Map;

/**
 * Captures HTTP request/response metadata and ships as HTTP_REQUEST telemetry.
 * Registered by LumoraAutoConfiguration.
 */
public class LumoraHttpInterceptor implements HandlerInterceptor {

    private static final String START_ATTR = "lumora_start";
    private final RingBufferQueue queue;

    public LumoraHttpInterceptor(RingBufferQueue queue) {
        this.queue = queue;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_ATTR, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        try {
            Long start = (Long) request.getAttribute(START_ATTR);
            long durationMs = start != null ? System.currentTimeMillis() - start : 0;

            Map<String, Object> payload = new HashMap<>();
            payload.put("method", request.getMethod());
            payload.put("path", request.getRequestURI());
            payload.put("status", response.getStatus());
            payload.put("duration_ms", durationMs);
            payload.put("user_agent", request.getHeader("User-Agent"));

            queue.offer("HTTP_REQUEST", payload);
        } catch (Exception e) {
            // Never crash the user's app
        }
    }
}
