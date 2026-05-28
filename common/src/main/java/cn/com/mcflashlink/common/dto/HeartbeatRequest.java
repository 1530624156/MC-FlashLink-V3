package cn.com.mcflashlink.common.dto;

public class HeartbeatRequest {
    private String roomCode;
    private String clientId;
    private String role;

    public HeartbeatRequest() {
    }

    public HeartbeatRequest(String roomCode, String clientId, String role) {
        this.roomCode = roomCode;
        this.clientId = clientId;
        this.role = role;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
