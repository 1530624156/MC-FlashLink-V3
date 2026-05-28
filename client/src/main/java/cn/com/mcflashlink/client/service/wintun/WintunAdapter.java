package cn.com.mcflashlink.client.service.wintun;

import com.sun.jna.Native;
import com.sun.jna.Pointer;

public class WintunAdapter implements AutoCloseable {
    private final WintunLibrary library;
    private final Pointer handle;
    private final String adapterName;
    private boolean closed;

    WintunAdapter(WintunLibrary library, Pointer handle, String adapterName) {
        this.library = library;
        this.handle = handle;
        this.adapterName = adapterName;
    }

    public WintunSession startSession(int ringCapacity) {
        Pointer session = library.WintunStartSession(handle, ringCapacity);
        if (session == null) {
            throw new WintunException("WintunStartSession failed, adapter=" + adapterName
                    + ", capacity=" + ringCapacity
                    + ", lastError=" + Native.getLastError());
        }
        return new WintunSession(library, session);
    }

    public boolean delete() {
        close();
        return false;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        library.WintunCloseAdapter(handle);
    }
}
