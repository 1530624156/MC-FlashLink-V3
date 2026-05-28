package cn.com.mcflashlink.server.controller;

import cn.com.mcflashlink.common.dto.HeartbeatRequest;
import cn.com.mcflashlink.common.dto.RoomCreateRequest;
import cn.com.mcflashlink.common.dto.RoomDismissRequest;
import cn.com.mcflashlink.common.dto.RoomInfoRequest;
import cn.com.mcflashlink.common.dto.RoomJoinRequest;
import cn.com.mcflashlink.common.dto.RoomLeaveRequest;
import cn.com.mcflashlink.common.dto.RoomResponse;
import cn.com.mcflashlink.server.service.RoomService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/room")
public class RoomController {
    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping("/create")
    public RoomResponse create(@RequestBody RoomCreateRequest request) {
        return roomService.createRoom(request);
    }

    @PostMapping("/join")
    public RoomResponse join(@RequestBody RoomJoinRequest request) {
        return roomService.joinRoom(request);
    }

    @PostMapping("/info")
    public RoomResponse info(@RequestBody RoomInfoRequest request) {
        return roomService.roomInfo(request);
    }

    @PostMapping("/leave")
    public RoomResponse leave(@RequestBody RoomLeaveRequest request) {
        return roomService.leaveRoom(request);
    }

    @PostMapping("/heartbeat")
    public RoomResponse heartbeat(@RequestBody HeartbeatRequest request) {
        return roomService.heartbeat(request);
    }

    @PostMapping("/dismiss")
    public RoomResponse dismiss(@RequestBody RoomDismissRequest request) {
        return roomService.dismissRoom(request);
    }
}
