package cn.com.mcflashlink.common.tunnel;

public enum TunnelPacketType {
    REGISTER((byte) 1),
    DATA((byte) 2),
    KEEPALIVE((byte) 3);

    private final byte code;

    TunnelPacketType(byte code) {
        this.code = code;
    }

    public byte getCode() {
        return code;
    }

    public static TunnelPacketType fromCode(byte code) {
        for (TunnelPacketType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown tunnel packet type: " + code);
    }
}
