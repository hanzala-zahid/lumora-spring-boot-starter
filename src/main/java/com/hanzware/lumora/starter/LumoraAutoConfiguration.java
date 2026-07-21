package com.hanzware.lumora.starter;

import ch.qos.logback.classic.LoggerContext;
import jakarta.annotation.PreDestroy;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@AutoConfiguration
@EnableConfigurationProperties(LumoraProperties.class)
@ConditionalOnProperty(prefix = "lumora", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LumoraAutoConfiguration implements WebMvcConfigurer {

    private final LumoraProperties props;
    private RingBufferQueue ringBuffer;

    public LumoraAutoConfiguration(LumoraProperties props) {
        this.props = props;
    }

    @Bean
    @ConditionalOnMissingBean
    public TelemetryClient lumoraTelemetryClient() {
        return new TelemetryClient(props);
    }

    @Bean
    @ConditionalOnMissingBean
    public RingBufferQueue lumoraRingBuffer(TelemetryClient client) {
        ringBuffer = new RingBufferQueue(client, props);
        attachLogbackAppender(ringBuffer);
        return ringBuffer;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (ringBuffer != null) {
            registry.addInterceptor(new LumoraHttpInterceptor(ringBuffer))
                    .excludePathPatterns("/actuator/**", "/error");
        }
    }

    @PreDestroy
    public void shutdown() {
        if (ringBuffer != null) ringBuffer.shutdown();
    }

    private void attachLogbackAppender(RingBufferQueue queue) {
        try {
            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            LumoraLogAppender appender = new LumoraLogAppender();
            appender.setQueue(queue);
            appender.setContext(context);
            appender.start();
            context.getLogger("ROOT").addAppender(appender);
        } catch (Exception e) {
            // Logback not on classpath or context unavailable — skip silently
        }
    }
}
