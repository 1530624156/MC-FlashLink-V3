package cn.com.mcflashlink.common.tunnel;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TunnelPacketTests {
    @Test
    void encodesAndDecodesDataPacket() {
        byte[] payload = "packet".getBytes(StandardCharsets.UTF_8);
        byte[] encoded = TunnelPacket.data("123456", "10.26.1.1", "10.26.1.2", payload).encode();

        TunnelPacket decoded = TunnelPacket.decode(encoded, encoded.length);

        assertEquals(TunnelPacketType.DATA, decoded.getType());
        assertEquals("123456", decoded.getRoomCode());
        assertEquals("10.26.1.1", decoded.getSourceVirtualIp());
        assertEquals("10.26.1.2", decoded.getTargetVirtualIp());
        assertArrayEquals(payload, decoded.getPayload());
    }
}
