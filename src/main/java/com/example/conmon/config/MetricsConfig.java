package com.example.conmon.config;

import com.example.conmon.monitor.ConnectionRegistry;
import com.example.conmon.monitor.EventStore;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    public MetricsConfig(MeterRegistry meterRegistry, ConnectionRegistry connectionRegistry, EventStore eventStore) {
        Gauge.builder("active_connections", connectionRegistry, ConnectionRegistry::size)
                .description("Current active monitored TCP connections")
                .register(meterRegistry);
    }
}
