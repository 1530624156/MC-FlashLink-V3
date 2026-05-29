package cn.com.mcflashlink.server.dto;

import java.util.List;

public class RoomOverviewResponse {
    private Integer roomCount;
    private Integer totalMemberCount;
    private Long generatedAtEpochMillis;
    private List<RoomOverview> rooms;

    public RoomOverviewResponse() {
    }

    public RoomOverviewResponse(Integer roomCount, Integer totalMemberCount, Long generatedAtEpochMillis, List<RoomOverview> rooms) {
        this.roomCount = roomCount;
        this.totalMemberCount = totalMemberCount;
        this.generatedAtEpochMillis = generatedAtEpochMillis;
        this.rooms = rooms;
    }

    public Integer getRoomCount() {
        return roomCount;
    }

    public void setRoomCount(Integer roomCount) {
        this.roomCount = roomCount;
    }

    public Integer getTotalMemberCount() {
        return totalMemberCount;
    }

    public void setTotalMemberCount(Integer totalMemberCount) {
        this.totalMemberCount = totalMemberCount;
    }

    public Long getGeneratedAtEpochMillis() {
        return generatedAtEpochMillis;
    }

    public void setGeneratedAtEpochMillis(Long generatedAtEpochMillis) {
        this.generatedAtEpochMillis = generatedAtEpochMillis;
    }

    public List<RoomOverview> getRooms() {
        return rooms;
    }

    public void setRooms(List<RoomOverview> rooms) {
        this.rooms = rooms;
    }
}
