package cn.com.mcflashlink.common.dto;

public class RoomResponse {
    private String status;
    private String message;
    private String roomCode;
    private String roomStatus;
    private Integer memberCount;
    private String virtualIp;
    private String hostVirtualIp;
    private String subnetCidr;
    private String tunnelMode;
    private Integer tunnelPort;

    public RoomResponse() {
    }

    public RoomResponse(
            String status,
            String message,
            String roomCode,
            String roomStatus,
            Integer memberCount,
            String virtualIp,
            String hostVirtualIp,
            String subnetCidr,
            String tunnelMode,
            Integer tunnelPort) {
        this.status = status;
        this.message = message;
        this.roomCode = roomCode;
        this.roomStatus = roomStatus;
        this.memberCount = memberCount;
        this.virtualIp = virtualIp;
        this.hostVirtualIp = hostVirtualIp;
        this.subnetCidr = subnetCidr;
        this.tunnelMode = tunnelMode;
        this.tunnelPort = tunnelPort;
    }

    public static RoomResponse success(
            String message,
            String roomCode,
            String roomStatus,
            Integer memberCount,
            String virtualIp,
            String hostVirtualIp,
            String subnetCidr,
            String tunnelMode,
            Integer tunnelPort) {
        return new RoomResponse("SUCCESS", message, roomCode, roomStatus, memberCount, virtualIp, hostVirtualIp, subnetCidr, tunnelMode, tunnelPort);
    }

    public static RoomResponse failure(String message) {
        return new RoomResponse("FAILURE", message, null, null, null, null, null, null, null, null);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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

    public Integer getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(Integer memberCount) {
        this.memberCount = memberCount;
    }

    public String getVirtualIp() {
        return virtualIp;
    }

    public void setVirtualIp(String virtualIp) {
        this.virtualIp = virtualIp;
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
}
