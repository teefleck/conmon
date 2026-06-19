package com.example.conmon.monitor;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ConnectionRegistry {

    private final ConcurrentHashMap<ConnectionKey, ActiveConnection> activeConnections = new ConcurrentHashMap<>();

    public DiffResult reconcile(Map<ConnectionKey, ActiveConnection> snapshot) {
        Set<ActiveConnection> connected = ConcurrentHashMap.newKeySet();
        Set<ActiveConnection> disconnected = ConcurrentHashMap.newKeySet();

        snapshot.forEach((key, current) -> activeConnections.compute(key, (ignored, existing) -> {
            if (existing == null) {
                connected.add(current);
                return current;
            }
            return existing.withLastSeenAt(current.lastSeenAt());
        }));

        activeConnections.forEach((key, active) -> {
            if (!snapshot.containsKey(key) && activeConnections.remove(key, active)) {
                disconnected.add(active);
            }
        });

        return new DiffResult(Set.copyOf(connected), Set.copyOf(disconnected));
    }

    public Collection<ActiveConnection> all() {
        return activeConnections.values().stream().toList();
    }

    public int size() {
        return activeConnections.size();
    }

    public void clear() {
        activeConnections.clear();
    }

    public record DiffResult(Set<ActiveConnection> connected, Set<ActiveConnection> disconnected) {
    }
}
