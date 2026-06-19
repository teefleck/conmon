package com.example.conmon.monitor;

public record ProcNetConnection(
        String localIp,
        int localPort,
        String remoteIp,
        int remotePort,
        TcpState state
) {
}
