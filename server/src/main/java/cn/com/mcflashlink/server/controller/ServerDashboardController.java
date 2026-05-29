package cn.com.mcflashlink.server.controller;

import cn.com.mcflashlink.server.dto.RoomOverviewResponse;
import cn.com.mcflashlink.server.service.RoomService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/server")
public class ServerDashboardController {
    private final RoomService roomService;

    public ServerDashboardController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping("/rooms")
    public RoomOverviewResponse rooms() {
        return roomService.roomOverviews();
    }
}
