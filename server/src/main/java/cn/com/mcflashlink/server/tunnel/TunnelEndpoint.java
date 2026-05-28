package cn.com.mcflashlink.server.tunnel;

import java.net.InetSocketAddress;
import java.time.Instant;

class TunnelEndpoint {
    private final InetSocketAddress address;
    private volatile Instant lastSeenAt;

    TunnelEndpoint(InetSocketAddress address) {
        this.address = address;
        this.lastSeenAt = Instant.now();
    }

    InetSocketAddress getAddress() {
        return address;
    }

    Instant getLastSeenAt() {
        return lastSeenAt;
    }

    void touch() {
        this.lastSeenAt = Instant.now();
    }
}
