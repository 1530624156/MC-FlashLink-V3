package cn.com.mcflashlink.server.tunnel;

import cn.com.mcflashlink.common.tunnel.TunnelPacket;
import org.junit.jupiter.api.Test;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class UdpTunnelRelayServerTests {
    @Test
    void relaysDataPacketToRegisteredTargetVirtualIp() throws Exception {
        UdpTunnelRelayServer relay = new UdpTunnelRelayServer(0);

        try (DatagramSocket host = new DatagramSocket();
             DatagramSocket guest = new DatagramSocket()) {
            relay.start();
            host.setSoTimeout(3000);
            guest.setSoTimeout(3000);

            int relayPort = relay.getLocalPort();
            send(host, relayPort, TunnelPacket.register("123456", "10.26.1.1"));
            send(guest, relayPort, TunnelPacket.register("123456", "10.26.1.2"));

            byte[] payload = "hello".getBytes(StandardCharsets.UTF_8);
            send(guest, relayPort, TunnelPacket.data("123456", "10.26.1.2", "10.26.1.1", payload));

            TunnelPacket received = receive(host);
            assertEquals("10.26.1.2", received.getSourceVirtualIp());
            assertEquals("10.26.1.1", received.getTargetVirtualIp());
            assertArrayEquals(payload, received.getPayload());
        } finally {
            relay.stop();
        }
    }

    private void send(DatagramSocket socket, int relayPort, TunnelPacket packet) throws Exception {
        byte[] encoded = packet.encode();
        socket.send(new DatagramPacket(encoded, encoded.length, InetAddress.getByName("127.0.0.1"), relayPort));
    }

    private TunnelPacket receive(DatagramSocket socket) throws Exception {
        byte[] buffer = new byte[TunnelPacket.MAX_PACKET_SIZE];
        DatagramPacket datagram = new DatagramPacket(buffer, buffer.length);
        socket.receive(datagram);
        return TunnelPacket.decode(datagram.getData(), datagram.getLength());
    }
}
