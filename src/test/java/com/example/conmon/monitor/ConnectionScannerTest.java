package com.example.conmon.monitor;

import com.example.conmon.config.MonitorProperties;
import com.example.conmon.entity.MonitoredClient;
import com.example.conmon.entity.MonitoredService;
import com.example.conmon.repository.MonitoredClientRepository;
import com.example.conmon.repository.MonitoredServiceRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConnectionScannerTest {

    @Test
    void generatesConnectedAndDisconnectedEvents() throws Exception {
        MonitorProperties properties = new MonitorProperties(false, 1000, 100, false, "/tmp/tcp", "/tmp/tcp6");
        ProcNetTcpParser parser = mock(ProcNetTcpParser.class);
        MonitoredServiceRepository serviceRepository = mock(MonitoredServiceRepository.class);
        MonitoredClientRepository clientRepository = mock(MonitoredClientRepository.class);
        EventStore eventStore = new EventStore(properties, mock(com.example.conmon.repository.ConnectionEventRepository.class), new SimpleMeterRegistry());
        Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        ConnectionScanner scanner = new ConnectionScanner(properties, parser, serviceRepository, clientRepository,
                new ConnectionMatcher(clock), new ConnectionRegistry(), eventStore, clock, new SimpleMeterRegistry());

        when(serviceRepository.findByEnabledTrue()).thenReturn(List.of(new MonitoredService("api", null, 8080, Set.of(), true)));
        when(clientRepository.findByEnabledTrue()).thenReturn(List.of(new MonitoredClient("worker", null, "10.0.0.42", Set.of(), true)));
        when(parser.parse(any(Path.class), eq(false)))
                .thenReturn(List.of(new ProcNetConnection("127.0.0.1", 8080, "10.0.0.42", 5555, TcpState.ESTABLISHED)))
                .thenReturn(List.of());
        when(parser.parse(any(Path.class), eq(true))).thenReturn(List.of());

        scanner.scanOnce();
        scanner.scanOnce();

        assertThat(eventStore.all()).extracting(ConnectionEvent::type)
                .containsExactly(ConnectionEventType.CONNECTED, ConnectionEventType.DISCONNECTED);
        assertThat(scanner.status().connectionEvents()).isEqualTo(2);
    }
}
