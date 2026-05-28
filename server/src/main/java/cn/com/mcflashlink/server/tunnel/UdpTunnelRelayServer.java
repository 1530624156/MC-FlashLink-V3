package cn.com.mcflashlink.server.tunnel;

import cn.com.mcflashlink.common.tunnel.TunnelPacket;
import cn.com.mcflashlink.common.tunnel.TunnelPacketType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class UdpTunnelRelayServer {
    private static final Duration ENDPOINT_TTL = Duration.ofSeconds(60);

    private final int udpPort;
    private final Map<String, TunnelEndpoint> endpoints = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private DatagramSocket socket;

    public UdpTunnelRelayServer(@Value("${mc-flash-link.tunnel.udp-port:21000}") int udpPort) {
        this.udpPort = udpPort;
    }

    @PostConstruct
    public void start() throws IOException {
        socket = new DatagramSocket(udpPort);
        running.set(true);
        executor.execute(this::receiveLoop);
    }

    int getLocalPort() {
        if (socket == null) {
            return udpPort;
        }
        return socket.getLocalPort();
    }

    @PreDestroy
    public void stop() {
        running.set(false);
        if (socket != null) {
            socket.close();
        }
        executor.shutdownNow();
    }

    public void removeRoom(String roomCode) {
        endpoints.keySet().removeIf(key -> key.startsWith(roomCode + "|"));
    }

    public void removeEndpoint(String roomCode, String virtualIp) {
        endpoints.remove(endpointKey(roomCode, virtualIp));
    }

    private void receiveLoop() {
        byte[] buffer = new byte[TunnelPacket.MAX_PACKET_SIZE];
        while (running.get()) {
            DatagramPacket datagram = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(datagram);
                TunnelPacket packet = TunnelPacket.decode(datagram.getData(), datagram.getLength());
                InetSocketAddress sender = new InetSocketAddress(datagram.getAddress(), datagram.getPort());
                handle(packet, sender);
                cleanupExpiredEndpoints();
            } catch (Exception exception) {
                if (running.get()) {
                    // Invalid or transient UDP packets are intentionally ignored.
                }
            }
        }
    }

    private void handle(TunnelPacket packet, InetSocketAddress sender) throws IOException {
        if (packet.getType() == TunnelPacketType.REGISTER || packet.getType() == TunnelPacketType.KEEPALIVE) {
            register(packet, sender);
            return;
        }
        if (packet.getType() == TunnelPacketType.DATA) {
            register(packet, sender);
            TunnelEndpoint target = endpoints.get(endpointKey(packet.getRoomCode(), packet.getTargetVirtualIp()));
            if (target != null) {
                byte[] encoded = packet.encode();
                socket.send(new DatagramPacket(encoded, encoded.length, target.getAddress()));
            }
        }
    }

    private void register(TunnelPacket packet, InetSocketAddress sender) {
        String key = endpointKey(packet.getRoomCode(), packet.getSourceVirtualIp());
        endpoints.compute(key, (ignored, endpoint) -> {
            if (endpoint == null || !endpoint.getAddress().equals(sender)) {
                return new TunnelEndpoint(sender);
            }
            endpoint.touch();
            return endpoint;
        });
    }

    private void cleanupExpiredEndpoints() {
        Instant now = Instant.now();
        endpoints.entrySet().removeIf(entry -> Duration.between(entry.getValue().getLastSeenAt(), now).compareTo(ENDPOINT_TTL) > 0);
    }

    private String endpointKey(String roomCode, String virtualIp) {
        return roomCode + "|" + virtualIp;
    }
}
