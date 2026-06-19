package com.example.conmon.monitor;

import com.example.conmon.entity.MonitoredClient;
import com.example.conmon.entity.MonitoredService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ConnectionMatcherTest {

    private final ConnectionMatcher matcher = new ConnectionMatcher(Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));

    @Test
    void matchesEstablishedConnectionByPortAndRemoteIp() {
        MonitoredService service = new MonitoredService("api", "desc", 8080, Set.of("prod"), true);
        MonitoredClient client = new MonitoredClient("worker", "desc", "10.0.0.42", Set.of("batch"), true);

        var active = matcher.match(
                new ProcNetConnection("127.0.0.1", 8080, "10.0.0.42", 5555, TcpState.ESTABLISHED),
                Map.of(8080, service),
                Map.of("10.0.0.42", client)
        );

        assertThat(active).isPresent();
        assertThat(active.orElseThrow().serviceName()).isEqualTo("api");
        assertThat(active.orElseThrow().clientName()).isEqualTo("worker");
        assertThat(active.orElseThrow().tags()).containsExactlyInAnyOrder("prod", "batch");
    }

    @Test
    void ignoresNonEstablishedOrUnconfiguredConnections() {
        assertThat(matcher.match(
                new ProcNetConnection("127.0.0.1", 8080, "10.0.0.42", 5555, TcpState.UNKNOWN),
                Map.of(),
                Map.of()
        )).isEmpty();
    }
}
