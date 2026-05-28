package cn.com.mcflashlink.common.dto;

public class RoomLeaveRequest {
    private String roomCode;
    private String guestId;

    public RoomLeaveRequest() {
    }

    public RoomLeaveRequest(String roomCode, String guestId) {
        this.roomCode = roomCode;
        this.guestId = guestId;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }

    public String getGuestId() {
        return guestId;
    }

    public void setGuestId(String guestId) {
        this.guestId = guestId;
    }
}
