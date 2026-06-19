package com.example.conmon.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ConfigurationUploadIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void uploadConfigurationImportsServicesAndClients() throws Exception {
        String document = objectMapper.writeValueAsString(Map.of(
                "monitor", Map.of(
                        "enabled", true,
                        "scanIntervalMillis", 1000,
                        "eventRetention", 10000,
                        "eventPersistenceEnabled", false,
                        "procNetTcpPath", "/proc/net/tcp",
                        "procNetTcp6Path", "/proc/net/tcp6"
                ),
                "services", List.of(Map.of(
                        "serviceName", "orders-api",
                        "description", "Orders API listener",
                        "port", 8080,
                        "tags", List.of("prod", "orders"),
                        "enabled", true
                )),
                "clients", List.of(Map.of(
                        "clientName", "payment-worker",
                        "description", "Payment worker host",
                        "ipAddress", "10.10.0.42",
                        "tags", List.of("prod", "payments"),
                        "enabled", true
                ))
        ));

        mockMvc.perform(post("/api/configuration?replaceExisting=true")
                        .contentType("application/json")
                        .content(document))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.services[0].serviceName").value("orders-api"))
                .andExpect(jsonPath("$.clients[0].clientName").value("payment-worker"));

        mockMvc.perform(get("/api/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].serviceName").value("orders-api"));
        mockMvc.perform(get("/api/clients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].clientName").value("payment-worker"));
    }
}
