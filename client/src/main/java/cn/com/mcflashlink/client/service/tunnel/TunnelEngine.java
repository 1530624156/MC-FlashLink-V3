package cn.com.mcflashlink.client.service.tunnel;

import cn.com.mcflashlink.client.service.wintun.WintunSession;
import cn.com.mcflashlink.common.tunnel.TunnelPacket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class TunnelEngine implements AutoCloseable {
    private final String roomCode;
    private final String virtualIp;
    private final InetSocketAddress relayAddress;
    private final WintunSession wintunSession;
    private final DatagramSocket socket;
    private final ExecutorService ioExecutor = Executors.newFixedThreadPool(2);
    private final ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();
    private final AtomicBoolean running = new AtomicBoolean(false);

    public TunnelEngine(String roomCode, String virtualIp, String relayHost, int relayPort, WintunSession wintunSession) throws IOException {
        this.roomCode = roomCode;
        this.virtualIp = virtualIp;
        this.relayAddress = new InetSocketAddress(InetAddress.getByName(relayHost), relayPort);
        this.wintunSession = wintunSession;
        this.socket = new DatagramSocket();
        this.socket.setSoTimeout(1000);
    }

    public void start() throws IOException {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        send(TunnelPacket.register(roomCode, virtualIp));
        heartbeatExecutor.scheduleWithFixedDelay(this::sendKeepaliveQuietly, 5, 5, TimeUnit.SECONDS);
        ioExecutor.execute(this::tunToUdpLoop);
        ioExecutor.execute(this::udpToTunLoop);
    }

    private void tunToUdpLoop() {
        while (running.get()) {
            try {
                byte[] ipPacket = wintunSession.receivePacket();
                String targetVirtualIp = ipv4Destination(ipPacket);
                if (targetVirtualIp != null && !targetVirtualIp.equals(virtualIp)) {
                    send(TunnelPacket.data(roomCode, virtualIp, targetVirtualIp, ipPacket));
                }
            } catch (Exception exception) {
                sleep(10);
            }
        }
    }

    private void udpToTunLoop() {
        byte[] buffer = new byte[TunnelPacket.MAX_PACKET_SIZE];
        while (running.get()) {
            DatagramPacket datagram = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(datagram);
                TunnelPacket packet = TunnelPacket.decode(datagram.getData(), datagram.getLength());
                if (roomCode.equals(packet.getRoomCode()) && virtualIp.equals(packet.getTargetVirtualIp())) {
                    wintunSession.sendPacket(packet.getPayload());
                }
            } catch (SocketTimeoutException ignored) {
            } catch (Exception exception) {
                sleep(10);
            }
        }
    }

    private void sendKeepaliveQuietly() {
        try {
            send(TunnelPacket.keepalive(roomCode, virtualIp));
        } catch (IOException ignored) {
        }
    }

    private void send(TunnelPacket packet) throws IOException {
        byte[] encoded = packet.encode();
        socket.send(new DatagramPacket(encoded, encoded.length, relayAddress));
    }

    private String ipv4Destination(byte[] packet) {
        if (packet.length < 20) {
            return null;
        }
        int version = (packet[0] >> 4) & 0x0F;
        if (version != 4) {
            return null;
        }
        byte[] destination = Arrays.copyOfRange(packet, 16, 20);
        return new StringBuilder()
                .append(destination[0] & 0xFF).append('.')
                .append(destination[1] & 0xFF).append('.')
                .append(destination[2] & 0xFF).append('.')
                .append(destination[3] & 0xFF)
                .toString();
    }

    @Override
    public void close() {
        running.set(false);
        socket.close();
        ioExecutor.shutdownNow();
        heartbeatExecutor.shutdownNow();
        wintunSession.close();
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
