package cn.com.mcflashlink.client.service.wintun;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.WString;

import java.util.Collections;

public class WintunDriver {
    private final WintunLibrary library;

    public WintunDriver() {
        this.library = Native.load(
                NativeWintunLibraryLoader.extractLibraryPath(),
                WintunLibrary.class,
                Collections.singletonMap("w32-unicode", true));
    }

    public WintunAdapter openOrCreateAdapter(String adapterName) {
        WString name = new WString(adapterName);
        Pointer adapter = openAdapterHandle(name);
        if (adapter == null) {
            adapter = library.WintunCreateAdapter(name, new WString("MC-FlashLink"), null);
        }
        if (adapter == null) {
            throw new WintunException("Unable to open or create Wintun adapter " + adapterName
                    + ", lastError=" + Native.getLastError());
        }
        return new WintunAdapter(library, adapter, adapterName);
    }

    public WintunAdapter openAdapter(String adapterName) {
        Pointer adapter = openAdapterHandle(new WString(adapterName));
        return adapter == null ? null : new WintunAdapter(library, adapter, adapterName);
    }

    private Pointer openAdapterHandle(WString name) {
        return library.WintunOpenAdapter(name);
    }
}
