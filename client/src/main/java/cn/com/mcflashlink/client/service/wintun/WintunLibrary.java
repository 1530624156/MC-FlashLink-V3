package cn.com.mcflashlink.client.service.wintun;

import com.sun.jna.Library;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.WinDef.DWORDByReference;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.win32.StdCallLibrary;

public interface WintunLibrary extends Library, StdCallLibrary {
    Pointer WintunCreateAdapter(WString name, WString tunnelType, Pointer requestedGuid);

    Pointer WintunOpenAdapter(WString name);

    void WintunCloseAdapter(Pointer adapter);

    Pointer WintunStartSession(Pointer adapter, int capacity);

    void WintunEndSession(Pointer session);

    Pointer WintunReceivePacket(Pointer session, DWORDByReference packetSize);

    void WintunReleaseReceivePacket(Pointer session, Pointer packet);

    Pointer WintunAllocateSendPacket(Pointer session, int packetSize);

    void WintunSendPacket(Pointer session, Pointer packet);

    HANDLE WintunGetReadWaitEvent(Pointer session);
}
