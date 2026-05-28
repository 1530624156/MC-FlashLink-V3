package cn.com.mcflashlink.common.dto;

public class RoomInfoRequest {
    private String roomCode;

    public RoomInfoRequest() {
    }

    public RoomInfoRequest(String roomCode) {
        this.roomCode = roomCode;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }
}
