package com.example.conmon.controller;

import com.example.conmon.dto.MonitorStatusResponse;
import com.example.conmon.monitor.ConnectionScanner;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/monitor")
@Tag(name = "Monitor")
public class MonitorController {

    private final ConnectionScanner scanner;

    public MonitorController(ConnectionScanner scanner) {
        this.scanner = scanner;
    }

    @GetMapping("/status")
    public MonitorStatusResponse status() {
        ConnectionScanner.MonitorSnapshot snapshot = scanner.status();
        return new MonitorStatusResponse(snapshot.lastScanTime(), snapshot.activeConnections(),
                snapshot.configuredServices(), snapshot.configuredClients(), snapshot.connectionEvents(),
                snapshot.connectedEvents(), snapshot.disconnectedEvents(), snapshot.lastScanDurationMs(),
                snapshot.lastError());
    }
}
