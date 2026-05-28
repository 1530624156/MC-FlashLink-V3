package cn.com.mcflashlink.client.model;

public class LocalClientStatus {
    private boolean success;
    private String message;
    private String connectionStatus;
    private String role;
    private String serverBaseUrl;
    private String clientId;
    private String roomCode;
    private String roomStatus;
    private Integer memberCount;
    private String virtualIp;
    private String hostVirtualIp;
    private String subnetCidr;
    private String tunnelMode;
    private String adapterStatus;
    private boolean canCreate;
    private boolean canDismiss;
    private boolean canJoin;
    private boolean canLeave;

    public static LocalClientStatus snapshot(
            boolean success,
            String message,
            String connectionStatus,
            String role,
            String serverBaseUrl,
            String clientId,
            String roomCode,
            String roomStatus,
            Integer memberCount,
            String virtualIp,
            String hostVirtualIp,
            String subnetCidr,
            String tunnelMode,
            String adapterStatus,
            boolean canCreate,
            boolean canDismiss,
            boolean canJoin,
            boolean canLeave) {
        LocalClientStatus status = new LocalClientStatus();
        status.success = success;
        status.message = message;
        status.connectionStatus = connectionStatus;
        status.role = role;
        status.serverBaseUrl = serverBaseUrl;
        status.clientId = clientId;
        status.roomCode = roomCode;
        status.roomStatus = roomStatus;
        status.memberCount = memberCount;
        status.virtualIp = virtualIp;
        status.hostVirtualIp = hostVirtualIp;
        status.subnetCidr = subnetCidr;
        status.tunnelMode = tunnelMode;
        status.adapterStatus = adapterStatus;
        status.canCreate = canCreate;
        status.canDismiss = canDismiss;
        status.canJoin = canJoin;
        status.canLeave = canLeave;
        return status;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getConnectionStatus() {
        return connectionStatus;
    }

    public String getRole() {
        return role;
    }

    public String getServerBaseUrl() {
        return serverBaseUrl;
    }

    public String getClientId() {
        return clientId;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public String getRoomStatus() {
        return roomStatus;
    }

    public Integer getMemberCount() {
        return memberCount;
    }

    public String getVirtualIp() {
        return virtualIp;
    }

    public String getHostVirtualIp() {
        return hostVirtualIp;
    }

    public String getSubnetCidr() {
        return subnetCidr;
    }

    public String getTunnelMode() {
        return tunnelMode;
    }

    public String getAdapterStatus() {
        return adapterStatus;
    }

    public boolean isCanCreate() {
        return canCreate;
    }

    public boolean isCanDismiss() {
        return canDismiss;
    }

    public boolean isCanJoin() {
        return canJoin;
    }

    public boolean isCanLeave() {
        return canLeave;
    }
}
