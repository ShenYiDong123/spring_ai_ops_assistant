package com.syd.ops.config.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 监控指标配置
 * 用于统计告警处理性能、成功率等关键指标
 */
@Configuration
public class MetricsConfig {

    @Bean
    public Counter alertProcessedCounter(MeterRegistry meterRegistry) {
        return Counter.builder("alert.processed.total")
                .description("Total number of alerts processed")
                .tag("application", "ai-ops-assistant")
                .register(meterRegistry);
    }

    @Bean
    public Counter alertErrorCounter(MeterRegistry meterRegistry) {
        return Counter.builder("alert.error.total")
                .description("Total number of alert processing errors")
                .tag("application", "ai-ops-assistant")
                .register(meterRegistry);
    }

    @Bean
    public Timer alertProcessingTimer(MeterRegistry meterRegistry) {
        return Timer.builder("alert.processing.duration")
                .description("Alert processing duration")
                .tag("application", "ai-ops-assistant")
                .register(meterRegistry);
    }

    @Bean
    public Counter ragQueryCounter(MeterRegistry meterRegistry) {
        return Counter.builder("rag.query.total")
                .description("Total number of RAG queries")
                .tag("application", "ai-ops-assistant")
                .register(meterRegistry);
    }

    @Bean
    public Timer ragQueryTimer(MeterRegistry meterRegistry) {
        return Timer.builder("rag.query.duration")
                .description("RAG query duration")
                .tag("application", "ai-ops-assistant")
                .register(meterRegistry);
    }
}