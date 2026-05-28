package cn.com.mcflashlink.client.service;

import cn.com.mcflashlink.client.model.LocalClientRequest;
import cn.com.mcflashlink.client.model.LocalClientStatus;
import cn.com.mcflashlink.common.dto.RoomResponse;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
public class LocalClientRoomService {
    private final RoomApiClient roomApiClient;
    private final VirtualNetworkAdapterService virtualNetworkAdapterService;
    private final ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();
    private final ScheduledExecutorService roomPollingExecutor = Executors.newSingleThreadScheduledExecutor();

    private ScheduledFuture<?> heartbeatTask;
    private ScheduledFuture<?> roomPollingTask;
    private String connectionStatus = "未连接";
    private String message = "本地控制台已就绪";
    private String role = "IDLE";
    private String serverBaseUrl = "http://127.0.0.1:8080";
    private String clientId;
    private String roomCode;
    private String roomStatus = "未创建";
    private Integer memberCount = 0;
    private String virtualIp = "-";
    private String hostVirtualIp = "-";
    private String subnetCidr = "-";
    private String tunnelMode = "-";

    public LocalClientRoomService(RoomApiClient roomApiClient, VirtualNetworkAdapterService virtualNetworkAdapterService) {
        this.roomApiClient = roomApiClient;
        this.virtualNetworkAdapterService = virtualNetworkAdapterService;
    }

    public synchronized LocalClientStatus status() {
        return snapshot(true, message);
    }

    public synchronized LocalClientStatus createRoom(LocalClientRequest request) {
        String baseUrl = trimmed(request == null ? null : request.getServerBaseUrl());
        String hostId = trimmed(request == null ? null : request.getClientId());
        if (isBlank(baseUrl)) {
            return fail("请输入服务地址");
        }
        if (isBlank(hostId)) {
            return fail("请输入房主标识");
        }

        try {
            stopHeartbeat();
            stopRoomPolling();
            RoomResponse response = roomApiClient.createRoom(baseUrl, hostId);
            if (!isSuccess(response)) {
                return fail(response.getMessage());
            }
            serverBaseUrl = baseUrl;
            clientId = hostId;
            role = "HOST";
            applyRoomResponse(response);
            startHeartbeat(baseUrl, response.getRoomCode(), hostId, "HOST");
            startRoomPolling(baseUrl, response.getRoomCode());
            String adapterResult = virtualNetworkAdapterService.start(baseUrl, response, hostId);
            message = "房间创建成功。虚拟网卡：" + adapterResult;
            connectionStatus = isAdapterFailure(adapterResult) ? "虚拟网卡异常" : "已连接服务端";
            return snapshot(!isAdapterFailure(adapterResult), message);
        } catch (Exception exception) {
            return fail("创建房间失败：" + exception.getMessage());
        }
    }

    public synchronized LocalClientStatus joinRoom(LocalClientRequest request) {
        String baseUrl = trimmed(request == null ? null : request.getServerBaseUrl());
        String targetRoomCode = trimmed(request == null ? null : request.getRoomCode());
        String guestId = trimmed(request == null ? null : request.getClientId());
        if (isBlank(baseUrl)) {
            return fail("请输入服务地址");
        }
        if (isBlank(targetRoomCode)) {
            return fail("请输入房间号");
        }
        if (isBlank(guestId)) {
            return fail("请输入访客标识");
        }

        try {
            stopHeartbeat();
            stopRoomPolling();
            RoomResponse response = roomApiClient.joinRoom(baseUrl, targetRoomCode, guestId);
            if (!isSuccess(response)) {
                return fail(response.getMessage());
            }
            serverBaseUrl = baseUrl;
            clientId = guestId;
            role = "GUEST";
            applyRoomResponse(response);
            startHeartbeat(baseUrl, response.getRoomCode(), guestId, "GUEST");
            startRoomPolling(baseUrl, response.getRoomCode());
            String adapterResult = virtualNetworkAdapterService.start(baseUrl, response, guestId);
            message = "加入成功。虚拟网卡：" + adapterResult;
            connectionStatus = isAdapterFailure(adapterResult) ? "虚拟网卡异常" : "已连接服务端";
            return snapshot(!isAdapterFailure(adapterResult), message);
        } catch (Exception exception) {
            return fail("加入房间失败：" + exception.getMessage());
        }
    }

    public synchronized LocalClientStatus leaveRoom() {
        if (!"GUEST".equals(role) || isBlank(roomCode) || isBlank(clientId)) {
            return fail("当前没有可退出的访客房间");
        }

        try {
            RoomResponse response = roomApiClient.leaveRoom(serverBaseUrl, roomCode, clientId);
            stopHeartbeat();
            stopRoomPolling();
            String uninstallResult = virtualNetworkAdapterService.uninstallAdapter();
            if (!isSuccess(response)) {
                resetRoomState("退出请求失败：" + response.getMessage() + "；" + uninstallResult);
                return snapshot(false, message);
            }
            resetRoomState("已退出房间。" + uninstallResult);
            return snapshot(true, message);
        } catch (Exception exception) {
            stopHeartbeat();
            stopRoomPolling();
            String uninstallResult = virtualNetworkAdapterService.uninstallAdapter();
            resetRoomState("退出房间异常：" + exception.getMessage() + "；" + uninstallResult);
            return snapshot(false, message);
        }
    }

    public synchronized LocalClientStatus dismissRoom() {
        if (!"HOST".equals(role) || isBlank(roomCode) || isBlank(clientId)) {
            return fail("当前没有可解散的房间");
        }

        try {
            RoomResponse response = roomApiClient.dismissRoom(serverBaseUrl, roomCode, clientId);
            stopHeartbeat();
            stopRoomPolling();
            String uninstallResult = virtualNetworkAdapterService.uninstallAdapter();
            if (!isSuccess(response)) {
                return fail("解散房间失败：" + response.getMessage() + "；" + uninstallResult);
            }
            resetRoomState("房间已解散。" + uninstallResult);
            return snapshot(true, message);
        } catch (Exception exception) {
            return fail("解散房间异常：" + exception.getMessage());
        }
    }

    @PreDestroy
    public synchronized void shutdown() {
        stopHeartbeat();
        stopRoomPolling();
        heartbeatExecutor.shutdownNow();
        roomPollingExecutor.shutdownNow();
        virtualNetworkAdapterService.close();
    }

    private void applyRoomResponse(RoomResponse response) {
        roomCode = valueOrDash(response.getRoomCode());
        roomStatus = formatRoomStatus(response.getRoomStatus());
        memberCount = response.getMemberCount();
        virtualIp = valueOrDash(response.getVirtualIp());
        hostVirtualIp = valueOrDash(response.getHostVirtualIp());
        subnetCidr = valueOrDash(response.getSubnetCidr());
        tunnelMode = formatTunnelMode(response.getTunnelMode());
        connectionStatus = "已连接服务端";
    }

    private synchronized void applyRoomPollingResponse(RoomResponse response) {
        if (isSuccess(response)) {
            connectionStatus = "已连接服务端";
            roomStatus = formatRoomStatus(response.getRoomStatus());
            memberCount = response.getMemberCount();
            hostVirtualIp = valueOrDash(response.getHostVirtualIp());
            subnetCidr = valueOrDash(response.getSubnetCidr());
            tunnelMode = formatTunnelMode(response.getTunnelMode());
            message = "房间状态已刷新";
        } else {
            connectionStatus = "房间状态异常";
            message = response.getMessage();
        }
    }

    private void resetRoomState(String newMessage) {
        connectionStatus = "未连接";
        role = "IDLE";
        clientId = null;
        roomCode = null;
        roomStatus = "未创建";
        memberCount = 0;
        virtualIp = "-";
        hostVirtualIp = "-";
        subnetCidr = "-";
        tunnelMode = "-";
        message = newMessage;
    }

    private synchronized void startHeartbeat(String baseUrl, String roomCode, String clientId, String role) {
        stopHeartbeat();
        heartbeatTask = heartbeatExecutor.scheduleWithFixedDelay(() -> {
            try {
                roomApiClient.heartbeat(baseUrl, roomCode, clientId, role);
            } catch (Exception ignored) {
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    private synchronized void stopHeartbeat() {
        if (heartbeatTask != null) {
            heartbeatTask.cancel(true);
            heartbeatTask = null;
        }
    }

    private synchronized void startRoomPolling(String baseUrl, String roomCode) {
        stopRoomPolling();
        roomPollingTask = roomPollingExecutor.scheduleWithFixedDelay(() -> {
            try {
                applyRoomPollingResponse(roomApiClient.roomInfo(baseUrl, roomCode));
            } catch (Exception exception) {
                synchronized (LocalClientRoomService.this) {
                    connectionStatus = "状态刷新失败";
                    message = exception.getMessage();
                }
            }
        }, 10, 10, TimeUnit.SECONDS);
    }

    private synchronized void stopRoomPolling() {
        if (roomPollingTask != null) {
            roomPollingTask.cancel(true);
            roomPollingTask = null;
        }
    }

    private LocalClientStatus fail(String failureMessage) {
        message = failureMessage;
        return snapshot(false, failureMessage);
    }

    private LocalClientStatus snapshot(boolean success, String snapshotMessage) {
        return LocalClientStatus.snapshot(
                success,
                snapshotMessage,
                connectionStatus,
                role,
                serverBaseUrl,
                clientId,
                roomCode,
                roomStatus,
                memberCount,
                virtualIp,
                hostVirtualIp,
                subnetCidr,
                tunnelMode,
                virtualNetworkAdapterService.statusText(),
                canCreate(),
                canDismiss(),
                canJoin(),
                canLeave());
    }

    private boolean canCreate() {
        return "IDLE".equals(role);
    }

    private boolean canDismiss() {
        return "HOST".equals(role);
    }

    private boolean canJoin() {
        return "IDLE".equals(role);
    }

    private boolean canLeave() {
        return "GUEST".equals(role);
    }

    private boolean isSuccess(RoomResponse response) {
        return response != null && "SUCCESS".equals(response.getStatus());
    }

    private boolean isAdapterFailure(String adapterResult) {
        return adapterResult != null && (adapterResult.contains("失败") || adapterResult.contains("异常"));
    }

    private String formatRoomStatus(String status) {
        if ("WAITING".equals(status)) {
            return "等待成员";
        }
        if ("CONNECTED".equals(status)) {
            return "已组网登记";
        }
        if ("CLOSED".equals(status)) {
            return "已关闭";
        }
        return valueOrDash(status);
    }

    private String formatTunnelMode(String tunnelMode) {
        if ("RELAY".equals(tunnelMode)) {
            return "UDP 服务器中转";
        }
        if ("P2P".equals(tunnelMode)) {
            return "P2P";
        }
        return valueOrDash(tunnelMode);
    }

    private String valueOrDash(String value) {
        return isBlank(value) ? "-" : value;
    }

    private String trimmed(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
