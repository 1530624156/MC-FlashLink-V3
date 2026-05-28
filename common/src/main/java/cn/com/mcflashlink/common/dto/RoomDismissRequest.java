package cn.com.mcflashlink.common.dto;

public class RoomDismissRequest {
    private String roomCode;
    private String hostId;

    public RoomDismissRequest() {
    }

    public RoomDismissRequest(String roomCode, String hostId) {
        this.roomCode = roomCode;
        this.hostId = hostId;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }

    public String getHostId() {
        return hostId;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }
}
