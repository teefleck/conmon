package com.example.conmon.monitor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ProcNetTcpParserTest {

    private final ProcNetTcpParser parser = new ProcNetTcpParser();

    @TempDir
    Path tempDir;

    @Test
    void parsesIpv4ProcNetTcp() throws Exception {
        Path procFile = tempDir.resolve("tcp");
        Files.writeString(procFile, """
                  sl  local_address rem_address   st tx_queue rx_queue tr tm->when retrnsmt   uid  timeout inode
                   0: 0100007F:1F90 2A00000A:C001 01 00000000:00000000 00:00000000 00000000 1000 0 1
                """);

        var connections = parser.parse(procFile, false);

        assertThat(connections).containsExactly(new ProcNetConnection(
                "127.0.0.1", 8080, "10.0.0.42", 49153, TcpState.ESTABLISHED));
    }

    @Test
    void parsesIpv6LoopbackProcNetTcp6() {
        String line = "0: 00000000000000000000000001000000:0050 00000000000000000000000002000000:C002 01 " +
                "00000000:00000000 00:00000000 00000000 1000 0 1";

        var parsed = parser.parseLine(line, true);

        assertThat(parsed).isPresent();
        assertThat(parsed.orElseThrow().localIp()).isEqualTo("0:0:0:0:0:0:0:1");
        assertThat(parsed.orElseThrow().remoteIp()).isEqualTo("0:0:0:0:0:0:0:2");
        assertThat(parsed.orElseThrow().localPort()).isEqualTo(80);
    }

    @Test
    void ignoresMalformedRows() {
        assertThat(parser.parseLine("bad row", false)).isEmpty();
    }
}
