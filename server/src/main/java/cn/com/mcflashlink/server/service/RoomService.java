package cn.com.mcflashlink.server.service;

import cn.com.mcflashlink.common.dto.HeartbeatRequest;
import cn.com.mcflashlink.common.dto.RoomCreateRequest;
import cn.com.mcflashlink.common.dto.RoomDismissRequest;
import cn.com.mcflashlink.common.dto.RoomInfoRequest;
import cn.com.mcflashlink.common.dto.RoomJoinRequest;
import cn.com.mcflashlink.common.dto.RoomLeaveRequest;
import cn.com.mcflashlink.common.dto.RoomResponse;
import cn.com.mcflashlink.server.dto.RoomOverview;
import cn.com.mcflashlink.server.dto.RoomOverviewResponse;
import cn.com.mcflashlink.server.model.RoomSession;
import cn.com.mcflashlink.server.model.RoomStatus;
import cn.com.mcflashlink.server.tunnel.UdpTunnelRelayServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Service
public class RoomService {
    private static final int ROOM_CODE_BOUND = 1_000_000;
    private static final int ROOM_CODE_RETRY_LIMIT = 20;
    private static final int SUBNET_MIN = 1;
    private static final int SUBNET_MAX = 254;
    private static final String TUNNEL_MODE = "RELAY";

    private final SecureRandom random = new SecureRandom();
    private final ConcurrentMap<String, RoomSession> rooms = new ConcurrentHashMap<>();
    private final Duration heartbeatTimeout;
    private final int tunnelPort;
    private final UdpTunnelRelayServer tunnelRelayServer;

    public RoomService(long heartbeatTimeoutSeconds, int tunnelPort) {
        this(heartbeatTimeoutSeconds, tunnelPort, null);
    }

    @Autowired
    public RoomService(
            @Value("${mc-flash-link.heartbeat-timeout-seconds:15}") long heartbeatTimeoutSeconds,
            @Value("${mc-flash-link.tunnel.udp-port:21000}") int tunnelPort,
            @Autowired(required = false) UdpTunnelRelayServer tunnelRelayServer) {
        this.heartbeatTimeout = Duration.ofSeconds(heartbeatTimeoutSeconds);
        this.tunnelPort = tunnelPort;
        this.tunnelRelayServer = tunnelRelayServer;
    }

    public RoomResponse createRoom(RoomCreateRequest request) {
        if (request == null || !StringUtils.hasText(request.getHostId())) {
            return RoomResponse.failure("hostId 不能为空");
        }
        String hostId = request.getHostId().trim();

        synchronized (rooms) {
            Integer subnetIndex = nextAvailableSubnetIndex();
            if (subnetIndex == null) {
                return RoomResponse.failure("没有可用的虚拟网段，请稍后重试");
            }

            for (int i = 0; i < ROOM_CODE_RETRY_LIMIT; i++) {
                String roomCode = randomRoomCode();
                if (rooms.containsKey(roomCode)) {
                    continue;
                }
                RoomSession session = new RoomSession(roomCode, hostId, subnetIndex);
                if (rooms.putIfAbsent(roomCode, session) == null) {
                    return toSuccessResponse("房间创建成功，已分配房主虚拟 IP", session, session.getHostVirtualIp());
                }
            }
        }
        return RoomResponse.failure("房间号生成失败，请稍后重试");
    }

    public RoomResponse joinRoom(RoomJoinRequest request) {
        if (request == null || !StringUtils.hasText(request.getRoomCode()) || !StringUtils.hasText(request.getGuestId())) {
            return RoomResponse.failure("roomCode 和 guestId 不能为空");
        }
        String roomCode = request.getRoomCode().trim();
        String guestId = request.getGuestId().trim();

        RoomSession session = rooms.get(roomCode);
        if (session == null || session.getStatus() == RoomStatus.CLOSED) {
            return RoomResponse.failure("房间不存在或已关闭");
        }

        String guestVirtualIp;
        synchronized (session) {
            try {
                guestVirtualIp = session.allocateGuestVirtualIp(guestId);
            } catch (IllegalStateException exception) {
                return RoomResponse.failure(exception.getMessage());
            }
            session.setLastGuestHeartbeatAt(Instant.now());
            session.setStatus(RoomStatus.CONNECTED);
        }

        return toSuccessResponse("加入成功，已分配访客虚拟 IP", session, guestVirtualIp);
    }

    public RoomResponse roomInfo(RoomInfoRequest request) {
        if (request == null || !StringUtils.hasText(request.getRoomCode())) {
            return RoomResponse.failure("roomCode 不能为空");
        }
        String roomCode = request.getRoomCode().trim();
        RoomSession session = rooms.get(roomCode);
        if (session == null || session.getStatus() == RoomStatus.CLOSED) {
            return RoomResponse.failure("房间不存在或已关闭");
        }
        return toSuccessResponse("房间状态刷新成功", session, null);
    }

    public RoomResponse leaveRoom(RoomLeaveRequest request) {
        if (request == null || !StringUtils.hasText(request.getRoomCode()) || !StringUtils.hasText(request.getGuestId())) {
            return RoomResponse.failure("roomCode 和 guestId 不能为空");
        }
        String roomCode = request.getRoomCode().trim();
        String guestId = request.getGuestId().trim();

        RoomSession session = rooms.get(roomCode);
        if (session == null || session.getStatus() == RoomStatus.CLOSED) {
            return RoomResponse.failure("房间不存在或已关闭");
        }

        String removedVirtualIp;
        synchronized (session) {
            if (!session.hasGuestId(guestId)) {
                return RoomResponse.failure("访客不在当前房间中");
            }
            removedVirtualIp = session.removeGuest(guestId);
            if (session.getGuestCount() == 0) {
                session.setStatus(RoomStatus.WAITING);
            }
        }
        removeTunnelEndpoint(roomCode, removedVirtualIp);
        return toSuccessResponse("已退出房间", session, removedVirtualIp);
    }

    public RoomResponse heartbeat(HeartbeatRequest request) {
        if (request == null || !StringUtils.hasText(request.getRoomCode()) || !StringUtils.hasText(request.getClientId())) {
            return RoomResponse.failure("roomCode 和 clientId 不能为空");
        }
        String roomCode = request.getRoomCode().trim();
        String clientId = request.getClientId().trim();

        RoomSession session = rooms.get(roomCode);
        if (session == null || session.getStatus() == RoomStatus.CLOSED) {
            return RoomResponse.failure("房间不存在或已关闭");
        }

        Instant now = Instant.now();
        String role = request.getRole() == null ? "" : request.getRole().toUpperCase(Locale.ROOT);
        if ("HOST".equals(role) || clientId.equals(session.getHostId())) {
            session.setLastHostHeartbeatAt(now);
        } else if ("GUEST".equals(role) || session.hasGuestId(clientId)) {
            session.setLastGuestHeartbeatAt(now);
        } else {
            return RoomResponse.failure("客户端身份与房间不匹配");
        }

        return toSuccessResponse("心跳成功", session, virtualIpFor(clientId, session));
    }

    public RoomResponse dismissRoom(RoomDismissRequest request) {
        if (request == null || !StringUtils.hasText(request.getRoomCode()) || !StringUtils.hasText(request.getHostId())) {
            return RoomResponse.failure("roomCode 和 hostId 不能为空");
        }
        String roomCode = request.getRoomCode().trim();
        String hostId = request.getHostId().trim();

        RoomSession session = rooms.get(roomCode);
        if (session == null || session.getStatus() == RoomStatus.CLOSED) {
            return RoomResponse.failure("房间不存在或已关闭");
        }
        if (!hostId.equals(session.getHostId())) {
            return RoomResponse.failure("只有房主可以解散房间");
        }

        session.setStatus(RoomStatus.CLOSED);
        rooms.remove(roomCode, session);
        removeTunnelEndpoints(roomCode);
        return RoomResponse.success(
                "房间已解散",
                session.getRoomCode(),
                RoomStatus.CLOSED.name(),
                0,
                session.getHostVirtualIp(),
                session.getHostVirtualIp(),
                session.getSubnetCidr(),
                TUNNEL_MODE,
                tunnelPort);
    }

    @Scheduled(fixedDelayString = "${mc-flash-link.room-clean-delay-ms:5000}")
    public void closeExpiredRooms() {
        Instant now = Instant.now();
        rooms.forEach((roomCode, session) -> {
            if (Duration.between(session.getLastHostHeartbeatAt(), now).compareTo(heartbeatTimeout) > 0) {
                session.setStatus(RoomStatus.CLOSED);
                rooms.remove(roomCode, session);
                removeTunnelEndpoints(roomCode);
            }
        });
    }

    int activeRoomCount() {
        return rooms.size();
    }

    public RoomOverviewResponse roomOverviews() {
        Instant now = Instant.now();
        List<RoomOverview> roomOverviews = rooms.values()
                .stream()
                .filter(session -> session.getStatus() != RoomStatus.CLOSED)
                .map(session -> toOverview(session, now))
                .sorted(Comparator.comparing(RoomOverview::getRoomCode))
                .collect(Collectors.toList());

        int totalMemberCount = roomOverviews.stream()
                .map(RoomOverview::getMemberCount)
                .filter(memberCount -> memberCount != null)
                .mapToInt(Integer::intValue)
                .sum();

        return new RoomOverviewResponse(roomOverviews.size(), totalMemberCount, now.toEpochMilli(), roomOverviews);
    }

    private String randomRoomCode() {
        return String.format("%06d", random.nextInt(ROOM_CODE_BOUND));
    }

    private Integer nextAvailableSubnetIndex() {
        int start = SUBNET_MIN + random.nextInt(SUBNET_MAX - SUBNET_MIN + 1);
        for (int offset = 0; offset <= SUBNET_MAX - SUBNET_MIN; offset++) {
            int candidate = SUBNET_MIN + ((start - SUBNET_MIN + offset) % (SUBNET_MAX - SUBNET_MIN + 1));
            if (!isSubnetAssigned(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean isSubnetAssigned(int subnetIndex) {
        for (RoomSession session : rooms.values()) {
            if (session.getSubnetIndex() == subnetIndex) {
                return true;
            }
        }
        return false;
    }

    private RoomResponse toSuccessResponse(String message, RoomSession session, String virtualIp) {
        return RoomResponse.success(
                message,
                session.getRoomCode(),
                session.getStatus().name(),
                memberCount(session),
                virtualIp,
                session.getHostVirtualIp(),
                session.getSubnetCidr(),
                TUNNEL_MODE,
                tunnelPort);
    }

    private int memberCount(RoomSession session) {
        return 1 + session.getGuestCount();
    }

    private RoomOverview toOverview(RoomSession session, Instant now) {
        return new RoomOverview(
                session.getRoomCode(),
                session.getStatus().name(),
                session.getHostId(),
                session.getHostVirtualIp(),
                session.getSubnetCidr(),
                memberCount(session),
                session.getGuestCount(),
                TUNNEL_MODE,
                tunnelPort,
                secondsBetween(session.getCreatedAt(), now),
                secondsBetween(session.getLastHostHeartbeatAt(), now),
                secondsBetween(session.getLastGuestHeartbeatAt(), now));
    }

    private Long secondsBetween(Instant start, Instant end) {
        if (start == null || end == null) {
            return null;
        }
        return Duration.between(start, end).getSeconds();
    }

    private String virtualIpFor(String clientId, RoomSession session) {
        if (clientId.equals(session.getHostId())) {
            return session.getHostVirtualIp();
        }
        return session.getGuestVirtualIp(clientId);
    }

    private void removeTunnelEndpoints(String roomCode) {
        if (tunnelRelayServer != null) {
            tunnelRelayServer.removeRoom(roomCode);
        }
    }

    private void removeTunnelEndpoint(String roomCode, String virtualIp) {
        if (tunnelRelayServer != null && virtualIp != null) {
            tunnelRelayServer.removeEndpoint(roomCode, virtualIp);
        }
    }
}
