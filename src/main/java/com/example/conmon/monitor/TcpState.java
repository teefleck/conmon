package com.example.conmon.monitor;

public enum TcpState {
    ESTABLISHED("01"),
    UNKNOWN("");

    private final String code;

    TcpState(String code) {
        this.code = code;
    }

    public static TcpState fromHex(String code) {
        return ESTABLISHED.code.equalsIgnoreCase(code) ? ESTABLISHED : UNKNOWN;
    }
}
