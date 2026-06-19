package com.example.conmon.controller;

import com.example.conmon.dto.ClientRequest;
import com.example.conmon.dto.ServiceRequest;
import com.example.conmon.monitor.ActiveConnection;
import com.example.conmon.monitor.ConnectionEvent;
import com.example.conmon.monitor.ConnectionEventType;
import com.example.conmon.monitor.ConnectionKey;
import com.example.conmon.monitor.ConnectionRegistry;
import com.example.conmon.monitor.ConnectionScanner;
import com.example.conmon.monitor.EventStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ApiIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    ConnectionRegistry registry;

    @Autowired
    EventStore eventStore;

    @Autowired
    ConnectionScanner scanner;

    @BeforeEach
    void setUp() {
        registry.clear();
    }

    @Test
    void serviceCrudWorks() throws Exception {
        String created = mockMvc.perform(post("/api/services")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new ServiceRequest("api", "desc", 8080, Set.of("prod"), true))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.serviceName").value("api"))
                .andExpect(jsonPath("$.port").value(8080))
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(created).get("id").asText();

        mockMvc.perform(get("/api/services/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));

        mockMvc.perform(put("/api/services/{id}", id)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new ServiceRequest("api-v2", "desc2", 8081, Set.of("blue"), false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceName").value("api-v2"))
                .andExpect(jsonPath("$.enabled").value(false));

        mockMvc.perform(delete("/api/services/{id}", id))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/services/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void clientCrudNormalizesIpAddress() throws Exception {
        String created = mockMvc.perform(post("/api/clients")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new ClientRequest("worker", "desc", "010.000.000.042", Set.of("batch"), true))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clientName").value("worker"))
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(created).get("id").asText();

        mockMvc.perform(get("/api/clients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(delete("/api/clients/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    void activeConnectionsAndEventsCanBeQueried() throws Exception {
        ActiveConnection active = activeConnection();
        registry.reconcile(Map.of(active.key(), active));
        eventStore.append(ConnectionEvent.of(ConnectionEventType.CONNECTED, active, Instant.parse("2026-01-01T00:00:00Z")));

        mockMvc.perform(get("/api/connections")
                        .param("serviceId", active.key().serviceId().toString())
                        .param("tags", "prod"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].serviceName").value("api"));

        mockMvc.perform(get("/api/events")
                        .param("clientId", active.key().clientId().toString())
                        .param("from", "2025-12-31T00:00:00Z")
                        .param("to", "2026-01-02T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].type").value("CONNECTED"));
    }

    @Test
    void monitorStatusReportsConfiguredEntitiesAfterManualScan() throws Exception {
        mockMvc.perform(post("/api/services")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new ServiceRequest("api", null, 8080, Set.of(), true))))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/clients")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new ClientRequest("worker", null, "10.0.0.42", Set.of(), true))))
                .andExpect(status().isCreated());

        scanner.scanOnce();

        mockMvc.perform(get("/api/monitor/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configuredServices").value(1))
                .andExpect(jsonPath("$.configuredClients").value(1));
    }

    @Test
    void configurationDocumentExportsServicesClientsAndMonitorSettings() throws Exception {
        mockMvc.perform(post("/api/services")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new ServiceRequest("config-api", null, 9090, Set.of("cfg"), true))))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/clients")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new ClientRequest("config-client", null, "192.0.2.10", Set.of("cfg"), true))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monitor.scanIntervalMillis").value(1000))
                .andExpect(jsonPath("$.services[0].serviceName").value("config-api"))
                .andExpect(jsonPath("$.clients[0].clientName").value("config-client"));
    }

    private ActiveConnection activeConnection() {
        ConnectionKey key = new ConnectionKey(UUID.randomUUID(), UUID.randomUUID(),
                "127.0.0.1", 8080, "10.0.0.42", 5555);
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        return new ActiveConnection(key, "api", "worker", Set.of("prod"), now, now);
    }
}
