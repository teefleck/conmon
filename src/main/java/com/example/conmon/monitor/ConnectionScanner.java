package com.example.conmon.monitor;

import com.example.conmon.config.MonitorProperties;
import com.example.conmon.entity.MonitoredClient;
import com.example.conmon.entity.MonitoredService;
import com.example.conmon.repository.MonitoredClientRepository;
import com.example.conmon.repository.MonitoredServiceRepository;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class ConnectionScanner {

    private final MonitorProperties properties;
    private final ProcNetTcpParser parser;
    private final MonitoredServiceRepository serviceRepository;
    private final MonitoredClientRepository clientRepository;
    private final ConnectionMatcher matcher;
    private final ConnectionRegistry registry;
    private final EventStore eventStore;
    private final Clock clock;
    private final DistributionSummary scanDuration;
    private final AtomicReference<MonitorSnapshot> status = new AtomicReference<>(MonitorSnapshot.empty());
    private ScheduledExecutorService executorService;

    public ConnectionScanner(MonitorProperties properties, ProcNetTcpParser parser,
                             MonitoredServiceRepository serviceRepository, MonitoredClientRepository clientRepository,
                             ConnectionMatcher matcher, ConnectionRegistry registry, EventStore eventStore,
                             Clock clock, MeterRegistry meterRegistry) {
        this.properties = properties;
        this.parser = parser;
        this.serviceRepository = serviceRepository;
        this.clientRepository = clientRepository;
        this.matcher = matcher;
        this.registry = registry;
        this.eventStore = eventStore;
        this.clock = clock;
        this.scanDuration = DistributionSummary.builder("scan_duration_ms")
                .description("TCP monitor scan duration in milliseconds")
                .baseUnit("milliseconds")
                .register(meterRegistry);
    }

    @EventListener(ApplicationReadyEvent.class)
    void start() {
        if (!properties.enabled()) {
            return;
        }
        if (executorService != null) {
            return;
        }
        executorService = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "connection-scanner");
            thread.setDaemon(true);
            return thread;
        });
        executorService.scheduleWithFixedDelay(this::scanSafely, 0, properties.scanIntervalMillis(), TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    void stop() {
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    public void scanOnce() {
        long started = System.nanoTime();
        List<MonitoredService> enabledServices = serviceRepository.findByEnabledTrue();
        List<MonitoredClient> enabledClients = clientRepository.findByEnabledTrue();
        Map<Integer, MonitoredService> servicesByPort = new HashMap<>(enabledServices.size());
        for (MonitoredService service : enabledServices) {
            servicesByPort.put(service.getPort(), service);
        }
        Map<String, MonitoredClient> clientsByIp = new HashMap<>(enabledClients.size());
        for (MonitoredClient client : enabledClients) {
            clientsByIp.put(client.getIpAddress(), client);
        }

        List<ProcNetConnection> procConnections = readProcConnections();
        Map<ConnectionKey, ActiveConnection> snapshot = new HashMap<>();
        for (ProcNetConnection procConnection : procConnections) {
            matcher.match(procConnection, servicesByPort, clientsByIp)
                    .ifPresent(active -> snapshot.put(active.key(), active));
        }

        Instant now = Instant.now(clock);
        ConnectionRegistry.DiffResult diff = registry.reconcile(snapshot);
        diff.connected().forEach(connection -> eventStore.append(ConnectionEvent.of(ConnectionEventType.CONNECTED, connection, now)));
        diff.disconnected().forEach(connection -> eventStore.append(ConnectionEvent.of(ConnectionEventType.DISCONNECTED, connection, now)));

        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
        scanDuration.record(elapsedMillis);
        status.set(new MonitorSnapshot(now, registry.size(), enabledServices.size(), enabledClients.size(),
                eventStore.totalEvents(), eventStore.connectedEvents(), eventStore.disconnectedEvents(), elapsedMillis, null));
    }

    public MonitorSnapshot status() {
        return status.get();
    }

    private void scanSafely() {
        try {
            scanOnce();
        } catch (RuntimeException ex) {
            status.set(status.get().withLastError(ex.getMessage()));
        }
    }

    private List<ProcNetConnection> readProcConnections() {
        try {
            List<ProcNetConnection> ipv4 = parser.parse(Path.of(properties.procNetTcpPath()), false);
            List<ProcNetConnection> ipv6 = parser.parse(Path.of(properties.procNetTcp6Path()), true);
            return java.util.stream.Stream.concat(ipv4.stream(), ipv6.stream()).toList();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse proc net tcp files", ex);
        }
    }

    public record MonitorSnapshot(Instant lastScanTime, int activeConnections, int configuredServices,
                                  int configuredClients, long connectionEvents, long connectedEvents,
                                  long disconnectedEvents, long lastScanDurationMs, String lastError) {
        static MonitorSnapshot empty() {
            return new MonitorSnapshot(null, 0, 0, 0, 0, 0, 0, 0, null);
        }

        MonitorSnapshot withLastError(String error) {
            return new MonitorSnapshot(lastScanTime, activeConnections, configuredServices, configuredClients,
                    connectionEvents, connectedEvents, disconnectedEvents, lastScanDurationMs, error);
        }
    }
}
