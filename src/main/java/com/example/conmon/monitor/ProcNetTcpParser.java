package com.example.conmon.monitor;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Component
public class ProcNetTcpParser {

    public List<ProcNetConnection> parse(Path path, boolean ipv6) throws IOException {
        if (!Files.exists(path)) {
            return List.of();
        }
        List<String> lines = Files.readAllLines(path);
        List<ProcNetConnection> connections = new ArrayList<>(Math.max(lines.size() - 1, 0));
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (!line.isEmpty()) {
                parseLine(line, ipv6).ifPresent(connections::add);
            }
        }
        return connections;
    }

    public java.util.Optional<ProcNetConnection> parseLine(String line, boolean ipv6) {
        String[] columns = line.trim().split("\\s+");
        if (columns.length < 4) {
            return java.util.Optional.empty();
        }
        try {
            AddressPort local = parseAddressPort(columns[1], ipv6);
            AddressPort remote = parseAddressPort(columns[2], ipv6);
            return java.util.Optional.of(new ProcNetConnection(
                    local.ip(),
                    local.port(),
                    remote.ip(),
                    remote.port(),
                    TcpState.fromHex(columns[3])
            ));
        } catch (IllegalArgumentException | UnknownHostException ex) {
            return java.util.Optional.empty();
        }
    }

    private AddressPort parseAddressPort(String value, boolean ipv6) throws UnknownHostException {
        String[] parts = value.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid address:port value");
        }
        int port = Integer.parseUnsignedInt(parts[1], 16);
        byte[] addressBytes = ipv6 ? parseIpv6(parts[0]) : parseIpv4(parts[0]);
        return new AddressPort(InetAddress.getByAddress(addressBytes).getHostAddress(), port);
    }

    private byte[] parseIpv4(String hex) {
        if (hex.length() != 8) {
            throw new IllegalArgumentException("Invalid IPv4 hex length");
        }
        byte[] raw = HexFormat.of().parseHex(hex);
        return new byte[]{raw[3], raw[2], raw[1], raw[0]};
    }

    private byte[] parseIpv6(String hex) {
        if (hex.length() != 32) {
            throw new IllegalArgumentException("Invalid IPv6 hex length");
        }
        byte[] raw = HexFormat.of().parseHex(hex);
        byte[] normalized = new byte[16];
        for (int word = 0; word < 4; word++) {
            int offset = word * 4;
            normalized[offset] = raw[offset + 3];
            normalized[offset + 1] = raw[offset + 2];
            normalized[offset + 2] = raw[offset + 1];
            normalized[offset + 3] = raw[offset];
        }
        return normalized;
    }

    private record AddressPort(String ip, int port) {
    }
}
