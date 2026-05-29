package cn.com.mcflashlink.server.dto;

public class RoomOverview {
    private String roomCode;
    private String roomStatus;
    private String hostId;
    private String hostVirtualIp;
    private String subnetCidr;
    private Integer memberCount;
    private Integer guestCount;
    private String tunnelMode;
    private Integer tunnelPort;
    private Long createdSecondsAgo;
    private Long lastHostHeartbeatSecondsAgo;
    private Long lastGuestHeartbeatSecondsAgo;

    public RoomOverview() {
    }

    public RoomOverview(
            String roomCode,
            String roomStatus,
            String hostId,
            String hostVirtualIp,
            String subnetCidr,
            Integer memberCount,
            Integer guestCount,
            String tunnelMode,
            Integer tunnelPort,
            Long createdSecondsAgo,
            Long lastHostHeartbeatSecondsAgo,
            Long lastGuestHeartbeatSecondsAgo) {
        this.roomCode = roomCode;
        this.roomStatus = roomStatus;
        this.hostId = hostId;
        this.hostVirtualIp = hostVirtualIp;
        this.subnetCidr = subnetCidr;
        this.memberCount = memberCount;
        this.guestCount = guestCount;
        this.tunnelMode = tunnelMode;
        this.tunnelPort = tunnelPort;
        this.createdSecondsAgo = createdSecondsAgo;
        this.lastHostHeartbeatSecondsAgo = lastHostHeartbeatSecondsAgo;
        this.lastGuestHeartbeatSecondsAgo = lastGuestHeartbeatSecondsAgo;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }

    public String getRoomStatus() {
        return roomStatus;
    }

    public void setRoomStatus(String roomStatus) {
        this.roomStatus = roomStatus;
    }

    public String getHostId() {
        return hostId;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }

    public String getHostVirtualIp() {
        return hostVirtualIp;
    }

    public void setHostVirtualIp(String hostVirtualIp) {
        this.hostVirtualIp = hostVirtualIp;
    }

    public String getSubnetCidr() {
        return subnetCidr;
    }

    public void setSubnetCidr(String subnetCidr) {
        this.subnetCidr = subnetCidr;
    }

    public Integer getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(Integer memberCount) {
        this.memberCount = memberCount;
    }

    public Integer getGuestCount() {
        return guestCount;
    }

    public void setGuestCount(Integer guestCount) {
        this.guestCount = guestCount;
    }

    public String getTunnelMode() {
        return tunnelMode;
    }

    public void setTunnelMode(String tunnelMode) {
        this.tunnelMode = tunnelMode;
    }

    public Integer getTunnelPort() {
        return tunnelPort;
    }

    public void setTunnelPort(Integer tunnelPort) {
        this.tunnelPort = tunnelPort;
    }

    public Long getCreatedSecondsAgo() {
        return createdSecondsAgo;
    }

    public void setCreatedSecondsAgo(Long createdSecondsAgo) {
        this.createdSecondsAgo = createdSecondsAgo;
    }

    public Long getLastHostHeartbeatSecondsAgo() {
        return lastHostHeartbeatSecondsAgo;
    }

    public void setLastHostHeartbeatSecondsAgo(Long lastHostHeartbeatSecondsAgo) {
        this.lastHostHeartbeatSecondsAgo = lastHostHeartbeatSecondsAgo;
    }

    public Long getLastGuestHeartbeatSecondsAgo() {
        return lastGuestHeartbeatSecondsAgo;
    }

    public void setLastGuestHeartbeatSecondsAgo(Long lastGuestHeartbeatSecondsAgo) {
        this.lastGuestHeartbeatSecondsAgo = lastGuestHeartbeatSecondsAgo;
    }
}
