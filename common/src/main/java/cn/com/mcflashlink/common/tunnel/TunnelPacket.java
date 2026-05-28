package cn.com.mcflashlink.common.tunnel;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class TunnelPacket {
    public static final int MAX_PACKET_SIZE = 65507;

    private static final int MAGIC = 0x4D43464C; // MCFL
    private static final byte VERSION = 1;

    private final TunnelPacketType type;
    private final String roomCode;
    private final String sourceVirtualIp;
    private final String targetVirtualIp;
    private final byte[] payload;

    public TunnelPacket(TunnelPacketType type, String roomCode, String sourceVirtualIp, String targetVirtualIp, byte[] payload) {
        this.type = type;
        this.roomCode = roomCode;
        this.sourceVirtualIp = sourceVirtualIp;
        this.targetVirtualIp = targetVirtualIp;
        this.payload = payload == null ? new byte[0] : payload;
    }

    public static TunnelPacket register(String roomCode, String sourceVirtualIp) {
        return new TunnelPacket(TunnelPacketType.REGISTER, roomCode, sourceVirtualIp, "", new byte[0]);
    }

    public static TunnelPacket keepalive(String roomCode, String sourceVirtualIp) {
        return new TunnelPacket(TunnelPacketType.KEEPALIVE, roomCode, sourceVirtualIp, "", new byte[0]);
    }

    public static TunnelPacket data(String roomCode, String sourceVirtualIp, String targetVirtualIp, byte[] payload) {
        return new TunnelPacket(TunnelPacketType.DATA, roomCode, sourceVirtualIp, targetVirtualIp, payload);
    }

    public byte[] encode() {
        byte[] room = bytes(roomCode);
        byte[] source = bytes(sourceVirtualIp);
        byte[] target = bytes(targetVirtualIp);
        int size = 4 + 1 + 1 + 1 + room.length + 1 + source.length + 1 + target.length + 4 + payload.length;
        ByteBuffer buffer = ByteBuffer.allocate(size);
        buffer.putInt(MAGIC);
        buffer.put(VERSION);
        buffer.put(type.getCode());
        putString(buffer, room);
        putString(buffer, source);
        putString(buffer, target);
        buffer.putInt(payload.length);
        buffer.put(payload);
        return buffer.array();
    }

    public static TunnelPacket decode(byte[] bytes, int length) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes, 0, length);
        if (buffer.remaining() < 12 || buffer.getInt() != MAGIC || buffer.get() != VERSION) {
            throw new IllegalArgumentException("Invalid tunnel packet header");
        }
        TunnelPacketType type = TunnelPacketType.fromCode(buffer.get());
        String roomCode = readString(buffer);
        String sourceVirtualIp = readString(buffer);
        String targetVirtualIp = readString(buffer);
        int payloadLength = buffer.getInt();
        if (payloadLength < 0 || payloadLength > buffer.remaining()) {
            throw new IllegalArgumentException("Invalid tunnel packet payload length");
        }
        byte[] payload = new byte[payloadLength];
        buffer.get(payload);
        return new TunnelPacket(type, roomCode, sourceVirtualIp, targetVirtualIp, payload);
    }

    public TunnelPacketType getType() {
        return type;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public String getSourceVirtualIp() {
        return sourceVirtualIp;
    }

    public String getTargetVirtualIp() {
        return targetVirtualIp;
    }

    public byte[] getPayload() {
        return payload;
    }

    private static void putString(ByteBuffer buffer, byte[] bytes) {
        if (bytes.length > 255) {
            throw new IllegalArgumentException("Tunnel string field is too long");
        }
        buffer.put((byte) bytes.length);
        buffer.put(bytes);
    }

    private static String readString(ByteBuffer buffer) {
        if (!buffer.hasRemaining()) {
            throw new IllegalArgumentException("Missing tunnel string field");
        }
        int length = buffer.get() & 0xFF;
        if (length > buffer.remaining()) {
            throw new IllegalArgumentException("Invalid tunnel string field length");
        }
        byte[] bytes = new byte[length];
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static byte[] bytes(String value) {
        return (value == null ? "" : value).getBytes(StandardCharsets.UTF_8);
    }
}
