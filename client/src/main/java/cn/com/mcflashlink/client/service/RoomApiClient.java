package cn.com.mcflashlink.client.service;

import cn.com.mcflashlink.common.dto.RoomCreateRequest;
import cn.com.mcflashlink.common.dto.RoomDismissRequest;
import cn.com.mcflashlink.common.dto.HeartbeatRequest;
import cn.com.mcflashlink.common.dto.RoomInfoRequest;
import cn.com.mcflashlink.common.dto.RoomJoinRequest;
import cn.com.mcflashlink.common.dto.RoomLeaveRequest;
import cn.com.mcflashlink.common.dto.RoomResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Component
public class RoomApiClient {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RoomResponse createRoom(String baseUrl, String hostId) throws IOException {
        return post(baseUrl, "/api/room/create", new RoomCreateRequest(hostId));
    }

    public RoomResponse joinRoom(String baseUrl, String roomCode, String guestId) throws IOException {
        return post(baseUrl, "/api/room/join", new RoomJoinRequest(roomCode, guestId));
    }

    public RoomResponse roomInfo(String baseUrl, String roomCode) throws IOException {
        return post(baseUrl, "/api/room/info", new RoomInfoRequest(roomCode));
    }

    public RoomResponse leaveRoom(String baseUrl, String roomCode, String guestId) throws IOException {
        return post(baseUrl, "/api/room/leave", new RoomLeaveRequest(roomCode, guestId));
    }

    public RoomResponse dismissRoom(String baseUrl, String roomCode, String hostId) throws IOException {
        return post(baseUrl, "/api/room/dismiss", new RoomDismissRequest(roomCode, hostId));
    }

    public RoomResponse heartbeat(String baseUrl, String roomCode, String clientId, String role) throws IOException {
        return post(baseUrl, "/api/room/heartbeat", new HeartbeatRequest(roomCode, clientId, role));
    }

    private RoomResponse post(String baseUrl, String path, Object request) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(normalizeBaseUrl(baseUrl) + path).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(3000);
        connection.setReadTimeout(5000);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("Accept", "application/json");

        byte[] body = objectMapper.writeValueAsBytes(request);
        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(body);
        }

        int statusCode = connection.getResponseCode();
        InputStream responseStream = statusCode >= 200 && statusCode < 300
                ? connection.getInputStream()
                : connection.getErrorStream();
        String responseBody = readBody(responseStream);
        if (statusCode < 200 || statusCode >= 300) {
            throw new IOException("服务端返回 HTTP " + statusCode + ": " + responseBody);
        }
        return objectMapper.readValue(responseBody, RoomResponse.class);
    }

    private String normalizeBaseUrl(String baseUrl) {
        String trimmed = baseUrl == null ? "" : baseUrl.trim();
        if (trimmed.endsWith("/")) {
            return trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String readBody(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        }
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        }
        return body.toString();
    }
}
