package cn.com.mcflashlink.common.dto;

public class RoomCreateRequest {
    private String hostId;

    public RoomCreateRequest() {
    }

    public RoomCreateRequest(String hostId) {
        this.hostId = hostId;
    }

    public String getHostId() {
        return hostId;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }
}
