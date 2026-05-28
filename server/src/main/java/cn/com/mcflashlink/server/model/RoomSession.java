package cn.com.mcflashlink.server.model;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RoomSession {
    private final String roomCode;
    private final String hostId;
    private final int subnetIndex;
    private final String subnetCidr;
    private final String hostVirtualIp;
    private final Set<String> guestIds = ConcurrentHashMap.newKeySet();
    private final Map<String, String> guestVirtualIps = new ConcurrentHashMap<>();
    private volatile RoomStatus status;
    private volatile Instant lastHostHeartbeatAt;
    private volatile Instant lastGuestHeartbeatAt;
    private volatile int nextGuestHostPart = 2;

    public RoomSession(String roomCode, String hostId, int subnetIndex) {
        this.roomCode = roomCode;
        this.hostId = hostId;
        this.subnetIndex = subnetIndex;
        this.subnetCidr = "10.26." + subnetIndex + ".0/24";
        this.hostVirtualIp = "10.26." + subnetIndex + ".1";
        this.status = RoomStatus.WAITING;
        this.lastHostHeartbeatAt = Instant.now();
    }

    public String getRoomCode() {
        return roomCode;
    }

    public String getHostId() {
        return hostId;
    }

    public int getSubnetIndex() {
        return subnetIndex;
    }

    public String getSubnetCidr() {
        return subnetCidr;
    }

    public String getHostVirtualIp() {
        return hostVirtualIp;
    }

    public synchronized String allocateGuestVirtualIp(String guestId) {
        guestIds.add(guestId);
        String existing = guestVirtualIps.get(guestId);
        if (existing != null) {
            return existing;
        }
        if (nextGuestHostPart > 254) {
            throw new IllegalStateException("房间虚拟 IP 地址池已耗尽");
        }
        String virtualIp = "10.26." + subnetIndex + "." + nextGuestHostPart++;
        guestVirtualIps.put(guestId, virtualIp);
        return virtualIp;
    }

    public boolean hasGuestId(String guestId) {
        return guestIds.contains(guestId);
    }

    public String getGuestVirtualIp(String guestId) {
        return guestVirtualIps.get(guestId);
    }

    public synchronized String removeGuest(String guestId) {
        String virtualIp = guestVirtualIps.remove(guestId);
        guestIds.remove(guestId);
        return virtualIp;
    }

    public int getGuestCount() {
        return guestIds.size();
    }

    public RoomStatus getStatus() {
        return status;
    }

    public void setStatus(RoomStatus status) {
        this.status = status;
    }

    public Instant getLastHostHeartbeatAt() {
        return lastHostHeartbeatAt;
    }

    public void setLastHostHeartbeatAt(Instant lastHostHeartbeatAt) {
        this.lastHostHeartbeatAt = lastHostHeartbeatAt;
    }

    public Instant getLastGuestHeartbeatAt() {
        return lastGuestHeartbeatAt;
    }

    public void setLastGuestHeartbeatAt(Instant lastGuestHeartbeatAt) {
        this.lastGuestHeartbeatAt = lastGuestHeartbeatAt;
    }
}
