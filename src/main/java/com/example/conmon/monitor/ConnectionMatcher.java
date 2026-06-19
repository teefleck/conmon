package com.example.conmon.monitor;

import com.example.conmon.entity.MonitoredClient;
import com.example.conmon.entity.MonitoredService;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class ConnectionMatcher {

    private final Clock clock;

    public ConnectionMatcher(Clock clock) {
        this.clock = clock;
    }

    public Optional<ActiveConnection> match(ProcNetConnection connection,
                                            Map<Integer, MonitoredService> servicesByPort,
                                            Map<String, MonitoredClient> clientsByIp) {
        if (connection.state() != TcpState.ESTABLISHED) {
            return Optional.empty();
        }
        MonitoredService service = servicesByPort.get(connection.localPort());
        MonitoredClient client = clientsByIp.get(connection.remoteIp());
        
        // If no client matches the remote IP, 
        // try if there is a wildcard client defined for
        // the local port ("ANY(port)")
        if (client == null) {
            String searchIP = "ANY("+connection.localPort()+")";
            client = clientsByIp.get(searchIP);
        }

        if (service == null || client == null) {
            return Optional.empty();
        }
        Instant now = Instant.now(clock);
        Set<String> tags = new LinkedHashSet<>(service.getTags());
        tags.addAll(client.getTags());
        ConnectionKey key = new ConnectionKey(
                service.getId(),
                client.getId(),
                connection.localIp(),
                connection.localPort(),
                connection.remoteIp(),
                connection.remotePort()
        );
        return Optional.of(new ActiveConnection(key, service.getServiceName(), client.getClientName(), Set.copyOf(tags), now, now));
    }
}
