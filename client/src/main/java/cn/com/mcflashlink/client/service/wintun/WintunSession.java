package cn.com.mcflashlink.client.service.wintun;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef.DWORDByReference;

public class WintunSession implements AutoCloseable {
    private final WintunLibrary library;
    private final Pointer handle;

    WintunSession(WintunLibrary library, Pointer handle) {
        this.library = library;
        this.handle = handle;
    }

    public byte[] receivePacket() {
        DWORDByReference sizeReference = new DWORDByReference();
        Pointer packet = library.WintunReceivePacket(handle, sizeReference);
        if (packet == null) {
            throw new WintunException("No Wintun packet available");
        }
        int size = sizeReference.getValue().intValue();
        byte[] bytes = packet.getByteArray(0, size);
        library.WintunReleaseReceivePacket(handle, packet);
        return bytes;
    }

    public void sendPacket(byte[] bytes) {
        Pointer packet = library.WintunAllocateSendPacket(handle, bytes.length);
        if (packet == null) {
            throw new WintunException("WintunAllocateSendPacket failed");
        }
        packet.write(0, bytes, 0, bytes.length);
        library.WintunSendPacket(handle, packet);
    }

    @Override
    public void close() {
        library.WintunEndSession(handle);
    }
}
