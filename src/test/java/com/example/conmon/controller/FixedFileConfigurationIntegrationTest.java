package com.example.conmon.controller;

import com.example.conmon.dto.ServiceRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "conmon.configuration.file=src/test/resources/conmon-config-test.json")
@AutoConfigureMockMvc
@ActiveProfiles({"test", "fixed-file"})
class FixedFileConfigurationIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void loadsFixedJsonConfigurationIntoReadOnlyInMemoryDatabase() throws Exception {
        mockMvc.perform(get("/api/configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readOnly").value(true))
                .andExpect(jsonPath("$.monitor.scanIntervalMillis").value(2500))
                .andExpect(jsonPath("$.services[0].id").value("00000000-0000-0000-0000-000000000101"))
                .andExpect(jsonPath("$.services[0].serviceName").value("fixed-api"))
                .andExpect(jsonPath("$.clients[0].clientName").value("fixed-client"));

        mockMvc.perform(post("/api/services")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new ServiceRequest("blocked", null, 8088, Set.of(), true))))
                .andExpect(status().isConflict());
    }
}
