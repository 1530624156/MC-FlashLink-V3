package cn.com.mcflashlink.client.api;

import cn.com.mcflashlink.client.model.LocalClientRequest;
import cn.com.mcflashlink.client.model.LocalClientStatus;
import cn.com.mcflashlink.client.service.LocalClientRoomService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/local")
public class LocalClientController {
    private final LocalClientRoomService roomService;

    public LocalClientController(LocalClientRoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping("/status")
    public LocalClientStatus status() {
        return roomService.status();
    }

    @PostMapping("/create-room")
    public LocalClientStatus createRoom(@RequestBody LocalClientRequest request) {
        return roomService.createRoom(request);
    }

    @PostMapping("/join-room")
    public LocalClientStatus joinRoom(@RequestBody LocalClientRequest request) {
        return roomService.joinRoom(request);
    }

    @PostMapping("/leave-room")
    public LocalClientStatus leaveRoom() {
        return roomService.leaveRoom();
    }

    @PostMapping("/dismiss-room")
    public LocalClientStatus dismissRoom() {
        return roomService.dismissRoom();
    }
}
