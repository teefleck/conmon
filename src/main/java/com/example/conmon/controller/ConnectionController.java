package com.example.conmon.controller;

import com.example.conmon.dto.ClientStatusResponse;
import com.example.conmon.dto.ConnectionResponse;
import com.example.conmon.dto.EventResponse;
import com.example.conmon.service.ConnectionQueryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@RestController
@Tag(name = "Connections")
public class ConnectionController {

    private final ConnectionQueryService queryService;

    public ConnectionController(ConnectionQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/api/connections")
    public List<ConnectionResponse> activeConnections(@RequestParam Optional<UUID> serviceId,
                                                       @RequestParam Optional<UUID> clientId,
                                                       @RequestParam(required = false) Set<String> tags) {
        return queryService.activeConnections(serviceId, clientId, tags);
    }

    @GetMapping("/api/events")
    public List<EventResponse> events(@RequestParam Optional<UUID> serviceId,
                                      @RequestParam Optional<UUID> clientId,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Optional<Instant> from,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Optional<Instant> to) {
        return queryService.events(serviceId, clientId, from, to);
    }

    @GetMapping("/api/client-status")
    public List<ClientStatusResponse> clientStatus(@RequestParam(required = false) String clientIp) {
        return queryService.clientStatus(Optional.ofNullable(clientIp));
    }
}
