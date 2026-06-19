package com.example.conmon.monitor;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ConnectionRegistryTest {

    private final ConnectionRegistry registry = new ConnectionRegistry();

    @Test
    void reportsConnectedAndDisconnectedDiffs() {
        ActiveConnection connection = connection();

        var first = registry.reconcile(Map.of(connection.key(), connection));
        var second = registry.reconcile(Map.of(connection.key(), connection.withLastSeenAt(Instant.parse("2026-01-01T00:00:01Z"))));
        var third = registry.reconcile(Map.of());

        assertThat(first.connected()).containsExactly(connection);
        assertThat(first.disconnected()).isEmpty();
        assertThat(second.connected()).isEmpty();
        assertThat(second.disconnected()).isEmpty();
        assertThat(third.connected()).isEmpty();
        assertThat(third.disconnected()).hasSize(1);
        assertThat(registry.size()).isZero();
    }

    private ActiveConnection connection() {
        ConnectionKey key = new ConnectionKey(UUID.randomUUID(), UUID.randomUUID(),
                "127.0.0.1", 8080, "10.0.0.42", 5555);
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        return new ActiveConnection(key, "api", "worker", Set.of("prod"), now, now);
    }
}
