# ConMon

ConMon is a Java 21 / Spring Boot 3 application that monitors Linux TCP connection lifecycle events by parsing `/proc/net/tcp` and `/proc/net/tcp6` once per second. It does not require application changes, packet capture, eBPF, Netlink, agents, or external monitoring software.

The monitor compares snapshots of `ESTABLISHED` sockets and emits:

- `CONNECTED` when a matching connection appears
- `DISCONNECTED` when a previously active matching connection disappears

Runtime active connection state is held only in memory. Service and client configuration is stored with JPA. Event persistence is disabled by default and can be enabled with `conmon.monitor.event-persistence-enabled=true`.

## Requirements

- Java 21
- Maven 3.9+
- Linux/RHEL host with readable `/proc/net/tcp` and `/proc/net/tcp6`

## Run

```bash
mvn spring-boot:run
```

Swagger UI is available at:

```text
http://localhost:8080/swagger-ui.html
```

Actuator endpoints include health and metrics:

```text
http://localhost:8080/actuator/health
http://localhost:8080/actuator/metrics
```

## API

- `GET|POST /api/services`
- `GET|PUT|DELETE /api/services/{id}`
- `GET|POST /api/clients`
- `GET|PUT|DELETE /api/clients/{id}`
- `GET /api/configuration`
- `GET /api/connections?serviceId={uuid}&clientId={uuid}&tags=prod&tags=db`
- `GET /api/events?serviceId={uuid}&clientId={uuid}&from=2026-01-01T00:00:00Z&to=2026-01-02T00:00:00Z`
- `GET /api/monitor/status`

Only connections matching both an enabled service port and an enabled client IP are reported.

A *wildcard* client matching all client IPs connecting to a given port can be configured by adding a client with `ANY(<port>)` as client IP.

## Example Service

```json
{
  "serviceName": "orders-api",
  "description": "Orders API listener",
  "port": 8080,
  "tags": ["prod", "orders"],
  "enabled": true
}
```

## Example Client

```json
{
  "clientName": "payment-worker",
  "description": "Payment worker host",
  "ipAddress": "10.10.0.42",
  "tags": ["prod", "payments"],
  "enabled": true
}
```

## Configuration

Defaults use in-memory H2:

```yaml
conmon:
  configuration:
    file:
    replace-existing: true
    read-only: false
  monitor:
    enabled: true
    scan-interval-millis: 1000
    event-retention: 10000
    event-persistence-enabled: false
    proc-net-tcp-path: /proc/net/tcp
    proc-net-tcp6-path: /proc/net/tcp6
```

Export the complete current configuration as JSON:

```bash
curl http://localhost:8080/api/configuration
```

Load configuration from a JSON file at startup:

```bash
java -jar target/conmon-0.0.1-SNAPSHOT.jar \
  --conmon.configuration.file=/path/to/conmon-config.json
```

The JSON file can include `monitor`, `services`, and `clients` sections. See `config/conmon-config.example.json`.

For fixed-file mode, use the `fixed-file` profile. This imports the JSON file into an embedded in-memory H2 database and makes service/client CRUD mutations read-only:

```bash
java -jar target/conmon-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=fixed-file \
  --conmon.configuration.file=/path/to/conmon-config.json
```

Use PostgreSQL with:

```bash
SPRING_PROFILES_ACTIVE=postgres mvn spring-boot:run
```

See `config/application-postgres.yml` and `docker-compose.yml`.

## Metrics

Micrometer metrics:

- `active_connections`
- `connection_events_total`
- `connected_events_total`
- `disconnected_events_total`
- `scan_duration_ms`

## Tests

```bash
mvn test
```

The Maven build is configured with JaCoCo and enforces at least 80% line coverage.
