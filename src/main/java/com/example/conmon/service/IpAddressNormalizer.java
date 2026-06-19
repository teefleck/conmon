package com.example.conmon.service;

import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Component
public class IpAddressNormalizer {

    public String normalize(String ipAddress) {
        try {
            if (ipAddress.startsWith("ANY(") && ipAddress.endsWith(")")) {
                // Special case for wildcard clients defined as "ANY(port)"
                return ipAddress;
            }
            return InetAddress.getByName(ipAddress).getHostAddress();
        } catch (UnknownHostException ex) {
            throw new IllegalArgumentException("Invalid IP address: " + ipAddress);
        }
    }
}
