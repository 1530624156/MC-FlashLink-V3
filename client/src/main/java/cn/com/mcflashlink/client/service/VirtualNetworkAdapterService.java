package cn.com.mcflashlink.client.service;

import cn.com.mcflashlink.client.service.tunnel.TunnelEngine;
import cn.com.mcflashlink.client.service.wintun.WintunAdapter;
import cn.com.mcflashlink.client.service.wintun.WintunDriver;
import cn.com.mcflashlink.client.service.wintun.WintunException;
import cn.com.mcflashlink.client.service.wintun.WintunSession;
import cn.com.mcflashlink.common.dto.RoomResponse;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Service
public class VirtualNetworkAdapterService implements AutoCloseable {
    private static final String ADAPTER_NAME_PREFIX = "MC-FL";
    private static final int WINTUN_RING_CAPACITY = 4 * 1024 * 1024;
    private static final int ADAPTER_READY_RETRY_COUNT = 20;
    private static final long ADAPTER_READY_RETRY_DELAY_MS = 500L;
    private static final int WINTUN_SESSION_RETRY_COUNT = 6;
    private static final long WINTUN_SESSION_RETRY_DELAY_MS = 300L;

    private WintunAdapter adapter;
    private TunnelEngine tunnelEngine;
    private String currentAdapterName;
    private String status = "未接入 Wintun";

    public synchronized String start(String serverBaseUrl, RoomResponse room, String clientId) {
        close();

        if (!isWindows()) {
            status = "仅 Windows 支持 Wintun";
            return status;
        }
        if (room.getTunnelPort() == null) {
            status = "服务端未返回 UDP 隧道端口";
            return status;
        }

        try {
            currentAdapterName = adapterNameFor(room);
            WintunDriver driver = new WintunDriver();
            adapter = openOrCreateAdapterWithRetry(driver);
            configureAdapter(currentAdapterName, room.getVirtualIp(), room.getSubnetCidr());
            WintunSession session = null;
            try {
                session = startSessionWithRecovery(driver, room);
                tunnelEngine = new TunnelEngine(
                        room.getRoomCode(),
                        room.getVirtualIp(),
                        new URL(serverBaseUrl).getHost(),
                        room.getTunnelPort(),
                        session);
                session = null;
            } finally {
                if (session != null) {
                    closeSessionQuietly(session);
                }
            }
            tunnelEngine.start();
            status = "Wintun 已启动（" + currentAdapterName + "）";
            return status;
        } catch (UnsatisfiedLinkError error) {
            status = "Wintun DLL 加载失败：" + error.getMessage();
            closeAfterStartFailure();
            return status;
        } catch (WintunException exception) {
            status = "Wintun 启动失败：" + exception.getMessage();
            closeAfterStartFailure();
            return status;
        } catch (Exception exception) {
            status = "隧道启动失败：" + exception.getMessage();
            closeAfterStartFailure();
            return status;
        }
    }

    public synchronized String statusText() {
        return status;
    }

    public synchronized String uninstallAdapter() {
        if (!isWindows()) {
            status = "仅 Windows 支持 Wintun";
            return status;
        }

        RuntimeException stopFailure = stopTunnelEngine();
        WintunAdapter adapterToDelete = adapter;
        String adapterNameToDelete = currentAdapterName;
        adapter = null;

        try {
            if (adapterNameToDelete == null || adapterNameToDelete.trim().isEmpty()) {
                adapterNameToDelete = ADAPTER_NAME_PREFIX;
            }
            if (adapterToDelete == null) {
                adapterToDelete = new WintunDriver().openAdapter(adapterNameToDelete);
            }
            if (adapterToDelete == null) {
                status = "虚拟网卡未安装";
                return appendStopFailure(status, stopFailure);
            }

            adapterToDelete.delete();
            adapterToDelete = null;
            String fallbackResult = removeAdapterDeviceByName(adapterNameToDelete);
            String waitResult = waitForAdapterRemoval(adapterNameToDelete);
            currentAdapterName = null;
            status = "虚拟网卡已卸载" + fallbackResult + waitResult;
            return appendStopFailure(status, stopFailure);
        } catch (UnsatisfiedLinkError error) {
            status = "卸载虚拟网卡失败：Wintun DLL 接口不兼容：" + error.getMessage();
            return appendStopFailure(status, stopFailure);
        } catch (RuntimeException exception) {
            status = "卸载虚拟网卡失败：" + exception.getMessage();
            return appendStopFailure(status, stopFailure);
        } finally {
            if (adapterToDelete != null) {
                try {
                    adapterToDelete.close();
                } catch (RuntimeException ignored) {
                }
            }
        }
    }

    @Override
    public synchronized void close() {
        RuntimeException closeFailure = stopTunnelEngine();
        if (adapter != null) {
            try {
                adapter.close();
            } catch (RuntimeException exception) {
                closeFailure = exception;
            }
            adapter = null;
        }
        if (closeFailure != null) {
            status = "停止异常：" + closeFailure.getMessage();
            return;
        }
        if (!"未接入 Wintun".equals(status)) {
            status = "已停止";
        }
    }

    private RuntimeException stopTunnelEngine() {
        RuntimeException closeFailure = null;
        if (tunnelEngine != null) {
            try {
                tunnelEngine.close();
            } catch (RuntimeException exception) {
                closeFailure = exception;
            }
            tunnelEngine = null;
        }
        return closeFailure;
    }

    private void closeAfterStartFailure() {
        String failureStatus = status;
        close();
        status = failureStatus;
    }

    private String appendStopFailure(String text, RuntimeException stopFailure) {
        if (stopFailure == null) {
            return text;
        }
        return text + "；停止隧道时出现异常：" + stopFailure.getMessage();
    }

    private void configureAdapter(String adapterName, String virtualIp, String subnetCidr) throws IOException, InterruptedException {
        String mask = maskFromCidr(subnetCidr);
        waitForAdapterInterface(adapterName);
        runNetshWithRetry("interface", "ipv4", "set", "address", "name=" + adapterName, "static", virtualIp, mask);
        runNetshWithRetry("interface", "ipv4", "set", "subinterface", adapterName, "mtu=1420", "store=active");
    }

    private WintunAdapter openOrCreateAdapterWithRetry(WintunDriver driver) throws InterruptedException {
        RuntimeException lastException = null;
        for (int attempt = 1; attempt <= ADAPTER_READY_RETRY_COUNT; attempt++) {
            try {
                return driver.openOrCreateAdapter(currentAdapterName);
            } catch (RuntimeException exception) {
                lastException = exception;
                sleepBeforeRetry();
            }
        }
        if (lastException != null) {
            throw lastException;
        }
        throw new WintunException("Unable to open or create Wintun adapter");
    }

    private WintunSession startSessionWithRecovery(WintunDriver driver, RoomResponse room) throws IOException, InterruptedException {
        try {
            return startSessionWithRetry();
        } catch (WintunException firstFailure) {
            closeAdapterQuietly();
            String cleanupResult = cleanupAdapterDevice(currentAdapterName);
            adapter = openOrCreateAdapterWithRetry(driver);
            configureAdapter(currentAdapterName, room.getVirtualIp(), room.getSubnetCidr());
            try {
                return startSessionWithRetry();
            } catch (WintunException secondFailure) {
                throw new WintunException("WintunStartSession failed after adapter rebuild: "
                        + secondFailure.getMessage()
                        + "; firstFailure=" + firstFailure.getMessage()
                        + cleanupResult,
                        secondFailure);
            }
        }
    }

    private WintunSession startSessionWithRetry() throws InterruptedException {
        WintunException lastException = null;
        for (int attempt = 1; attempt <= WINTUN_SESSION_RETRY_COUNT; attempt++) {
            try {
                return adapter.startSession(WINTUN_RING_CAPACITY);
            } catch (WintunException exception) {
                lastException = exception;
                sleepBeforeSessionRetry();
            }
        }
        throw lastException == null
                ? new WintunException("WintunStartSession failed")
                : lastException;
    }

    private String cleanupAdapterDevice(String adapterName) {
        if (adapterName == null || adapterName.trim().isEmpty()) {
            return "";
        }
        return removeAdapterDeviceByName(adapterName) + waitForAdapterRemoval(adapterName);
    }

    private void closeAdapterQuietly() {
        if (adapter == null) {
            return;
        }
        try {
            adapter.close();
        } catch (RuntimeException ignored) {
        } finally {
            adapter = null;
        }
    }

    private void closeSessionQuietly(WintunSession session) {
        try {
            session.close();
        } catch (RuntimeException ignored) {
        }
    }

    private void waitForAdapterInterface(String adapterName) throws IOException, InterruptedException {
        for (int attempt = 1; attempt <= ADAPTER_READY_RETRY_COUNT; attempt++) {
            if (isAdapterInterfacePresent(adapterName)) {
                return;
            }
            sleepBeforeRetry();
        }
        throw new IOException("虚拟网卡已创建，但 Windows 网络接口尚未就绪");
    }

    private void runNetshWithRetry(String... args) throws IOException, InterruptedException {
        IOException lastException = null;
        for (int attempt = 1; attempt <= ADAPTER_READY_RETRY_COUNT; attempt++) {
            try {
                runNetsh(args);
                return;
            } catch (IOException exception) {
                lastException = exception;
                sleepBeforeRetry();
            }
        }
        throw new IOException("netsh 多次执行失败：" + (lastException == null ? "未知错误" : lastException.getMessage()), lastException);
    }

    private void runNetsh(String... args) throws IOException, InterruptedException {
        String[] command = new String[args.length + 1];
        command[0] = "netsh";
        System.arraycopy(args, 0, command, 1, args.length);
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = readProcessOutput(process);
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("netsh 执行失败，退出码 " + exitCode + valueSuffix(output));
        }
    }

    private boolean isAdapterInterfacePresent(String adapterName) {
        try {
            Process process = new ProcessBuilder(
                    "netsh",
                    "interface",
                    "show",
                    "interface",
                    "name=" + adapterName)
                    .redirectErrorStream(true)
                    .start();
            process.waitFor();
            return process.exitValue() == 0;
        } catch (Exception exception) {
            return false;
        }
    }

    private String waitForAdapterRemoval(String adapterName) {
        for (int attempt = 1; attempt <= ADAPTER_READY_RETRY_COUNT; attempt++) {
            if (!isAdapterInterfacePresent(adapterName)) {
                return "";
            }
            try {
                sleepBeforeRetry();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return "；等待网卡移除时被中断";
            }
        }
        return "；Windows 仍在后台移除旧网卡，若马上再次创建失败请稍等几秒重试";
    }

    private void sleepBeforeRetry() throws InterruptedException {
        Thread.sleep(ADAPTER_READY_RETRY_DELAY_MS);
    }

    private void sleepBeforeSessionRetry() throws InterruptedException {
        Thread.sleep(WINTUN_SESSION_RETRY_DELAY_MS);
    }

    private String removeAdapterDeviceByName(String adapterName) {
        String script = "$devices = Get-PnpDevice -Class Net -ErrorAction SilentlyContinue "
                + "| Where-Object { $_.FriendlyName -eq '" + adapterName + "' }; "
                + "foreach ($device in $devices) { "
                + "pnputil /remove-device $device.InstanceId | Out-Null; "
                + "if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE } "
                + "}; exit 0";
        try {
            Process process = new ProcessBuilder(
                    "powershell.exe",
                    "-NoProfile",
                    "-ExecutionPolicy",
                    "Bypass",
                    "-Command",
                    script)
                    .redirectErrorStream(true)
                    .start();
            String output = readProcessOutput(process);
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                return "";
            }
            return "；设备清理命令返回 " + exitCode + valueSuffix(output);
        } catch (Exception exception) {
            return "；设备清理命令未执行：" + exception.getMessage();
        }
    }

    private String readProcessOutput(Process process) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (output.length() > 0) {
                    output.append(' ');
                }
                output.append(line);
            }
        }
        return output.toString();
    }

    private String valueSuffix(String value) {
        return value == null || value.trim().isEmpty() ? "" : "：" + value.trim();
    }

    private String adapterNameFor(RoomResponse room) {
        String roomCode = sanitizeNamePart(room.getRoomCode());
        String hostPart = "x";
        String virtualIp = room.getVirtualIp();
        if (virtualIp != null && virtualIp.contains(".")) {
            hostPart = sanitizeNamePart(virtualIp.substring(virtualIp.lastIndexOf('.') + 1));
        }
        return ADAPTER_NAME_PREFIX + "-" + roomCode + "-" + hostPart;
    }

    private String sanitizeNamePart(String value) {
        if (value == null) {
            return "x";
        }
        String sanitized = value.replaceAll("[^A-Za-z0-9_-]", "");
        return sanitized.isEmpty() ? "x" : sanitized;
    }

    private String maskFromCidr(String subnetCidr) {
        int prefixLength = 24;
        if (subnetCidr != null && subnetCidr.contains("/")) {
            prefixLength = Integer.parseInt(subnetCidr.substring(subnetCidr.indexOf('/') + 1));
        }
        int mask = prefixLength == 0 ? 0 : 0xFFFFFFFF << (32 - prefixLength);
        return ((mask >>> 24) & 0xFF) + "."
                + ((mask >>> 16) & 0xFF) + "."
                + ((mask >>> 8) & 0xFF) + "."
                + (mask & 0xFF);
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }
}
