package cn.com.mcflashlink.server.service;

import cn.com.mcflashlink.common.dto.HeartbeatRequest;
import cn.com.mcflashlink.common.dto.RoomDismissRequest;
import cn.com.mcflashlink.common.dto.RoomCreateRequest;
import cn.com.mcflashlink.common.dto.RoomInfoRequest;
import cn.com.mcflashlink.common.dto.RoomJoinRequest;
import cn.com.mcflashlink.common.dto.RoomLeaveRequest;
import cn.com.mcflashlink.common.dto.RoomResponse;
import cn.com.mcflashlink.server.dto.RoomOverviewResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoomServiceTests {
    @Test
    void createsRoomAndAllocatesVirtualIps() {
        RoomService roomService = new RoomService(15, 21000);

        RoomResponse create = roomService.createRoom(new RoomCreateRequest("host-1"));
        assertEquals("SUCCESS", create.getStatus());
        assertNotNull(create.getRoomCode());
        assertTrue(create.getVirtualIp().matches("10\\.26\\.\\d+\\.1"));
        assertEquals(create.getVirtualIp(), create.getHostVirtualIp());
        assertEquals(Integer.valueOf(21000), create.getTunnelPort());
        assertEquals(1, create.getMemberCount());

        RoomResponse firstGuest = roomService.joinRoom(new RoomJoinRequest(create.getRoomCode(), "guest-1"));
        assertEquals("SUCCESS", firstGuest.getStatus());
        assertTrue(firstGuest.getVirtualIp().matches("10\\.26\\.\\d+\\.2"));
        assertEquals(create.getSubnetCidr(), firstGuest.getSubnetCidr());
        assertEquals(2, firstGuest.getMemberCount());

        RoomResponse secondGuest = roomService.joinRoom(new RoomJoinRequest(create.getRoomCode(), "guest-2"));
        assertEquals("SUCCESS", secondGuest.getStatus());
        assertTrue(secondGuest.getVirtualIp().matches("10\\.26\\.\\d+\\.3"));
        assertEquals(create.getSubnetCidr(), secondGuest.getSubnetCidr());
        assertEquals(3, secondGuest.getMemberCount());
        assertNotEquals(firstGuest.getVirtualIp(), secondGuest.getVirtualIp());

        RoomResponse roomInfo = roomService.roomInfo(new RoomInfoRequest(create.getRoomCode()));
        assertEquals("SUCCESS", roomInfo.getStatus());
        assertEquals(3, roomInfo.getMemberCount());
        assertEquals(create.getHostVirtualIp(), roomInfo.getHostVirtualIp());
    }

    @Test
    void onlyHostCanDismissRoom() {
        RoomService roomService = new RoomService(15, 21000);
        RoomResponse create = roomService.createRoom(new RoomCreateRequest("host-1"));

        RoomResponse rejected = roomService.dismissRoom(new RoomDismissRequest(create.getRoomCode(), "guest-1"));
        assertEquals("FAILURE", rejected.getStatus());
        assertEquals("只有房主可以解散房间", rejected.getMessage());

        RoomResponse dismissed = roomService.dismissRoom(new RoomDismissRequest(create.getRoomCode(), "host-1"));
        assertEquals("SUCCESS", dismissed.getStatus());
        assertEquals("CLOSED", dismissed.getRoomStatus());
        assertEquals(0, dismissed.getMemberCount());

        RoomResponse joinAfterDismiss = roomService.joinRoom(new RoomJoinRequest(create.getRoomCode(), "guest-1"));
        assertEquals("FAILURE", joinAfterDismiss.getStatus());
    }

    @Test
    void guestCanLeaveRoom() {
        RoomService roomService = new RoomService(15, 21000);
        RoomResponse create = roomService.createRoom(new RoomCreateRequest("host-1"));
        RoomResponse join = roomService.joinRoom(new RoomJoinRequest(create.getRoomCode(), "guest-1"));
        assertEquals("SUCCESS", join.getStatus());
        assertEquals(2, join.getMemberCount());

        RoomResponse left = roomService.leaveRoom(new RoomLeaveRequest(create.getRoomCode(), "guest-1"));
        assertEquals("SUCCESS", left.getStatus());
        assertEquals("WAITING", left.getRoomStatus());
        assertEquals(1, left.getMemberCount());

        RoomResponse leaveAgain = roomService.leaveRoom(new RoomLeaveRequest(create.getRoomCode(), "guest-1"));
        assertEquals("FAILURE", leaveAgain.getStatus());
    }

    @Test
    void exposesRoomOverviewForDashboard() {
        RoomService roomService = new RoomService(15, 21000);
        RoomResponse create = roomService.createRoom(new RoomCreateRequest("host-1"));
        roomService.joinRoom(new RoomJoinRequest(create.getRoomCode(), "guest-1"));

        RoomOverviewResponse overview = roomService.roomOverviews();

        assertEquals(1, overview.getRoomCount());
        assertEquals(2, overview.getTotalMemberCount());
        assertEquals(create.getRoomCode(), overview.getRooms().get(0).getRoomCode());
        assertEquals("host-1", overview.getRooms().get(0).getHostId());
        assertEquals("CONNECTED", overview.getRooms().get(0).getRoomStatus());
        assertEquals(Integer.valueOf(1), overview.getRooms().get(0).getGuestCount());
        assertEquals(Integer.valueOf(21000), overview.getRooms().get(0).getTunnelPort());
        assertNotNull(overview.getGeneratedAtEpochMillis());
    }

    @Test
    void dashboardOverviewDoesNotIncludeDismissedRooms() {
        RoomService roomService = new RoomService(15, 21000);
        RoomResponse create = roomService.createRoom(new RoomCreateRequest("host-1"));
        roomService.dismissRoom(new RoomDismissRequest(create.getRoomCode(), "host-1"));

        RoomOverviewResponse overview = roomService.roomOverviews();

        assertEquals(0, overview.getRoomCount());
        assertEquals(0, overview.getTotalMemberCount());
    }

    @Test
    void joinsRoomWithTrimmedCodeAndHostHeartbeat() throws Exception {
        RoomService roomService = new RoomService(1, 21000);
        RoomResponse create = roomService.createRoom(new RoomCreateRequest(" host-1 "));

        Thread.sleep(600);
        RoomResponse heartbeat = roomService.heartbeat(new HeartbeatRequest(" " + create.getRoomCode() + " ", " host-1 ", "HOST"));
        assertEquals("SUCCESS", heartbeat.getStatus());

        Thread.sleep(600);
        roomService.closeExpiredRooms();

        RoomResponse join = roomService.joinRoom(new RoomJoinRequest(" " + create.getRoomCode() + " ", " guest-1 "));
        assertEquals("SUCCESS", join.getStatus());
        assertTrue(join.getVirtualIp().matches("10\\.26\\.\\d+\\.2"));
    }
}
