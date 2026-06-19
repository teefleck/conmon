package com.example.conmon.monitor;

import java.util.UUID;

public record ConnectionKey(
        UUID serviceId,
        UUID clientId,
        String localIp,
        int localPort,
        String remoteIp,
        int remotePort
) {
}
