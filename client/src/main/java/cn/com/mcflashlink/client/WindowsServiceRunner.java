package cn.com.mcflashlink.client;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Advapi32;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.Winsvc;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public final class WindowsServiceRunner {
    private static final String SERVICE_NAME = "MCFlashLinkClient";
    private static final int ERROR_FAILED_SERVICE_CONTROLLER_CONNECT = 1063;

    private static volatile Winsvc.SERVICE_STATUS_HANDLE serviceStatusHandle;
    private static volatile ConfigurableApplicationContext applicationContext;
    private static volatile CountDownLatch stopLatch;

    private static Winsvc.SERVICE_MAIN_FUNCTION serviceMainCallback;
    private static Winsvc.HandlerEx serviceControlHandler;

    private WindowsServiceRunner() {
    }

    public static void run(String[] args) {
        if (!isWindows()) {
            McFlashLinkClientApplication.runApplication(serviceArgs(args));
            return;
        }

        stopLatch = new CountDownLatch(1);
        serviceMainCallback = (argc, argv) -> runServiceMain(args);

        Winsvc.SERVICE_TABLE_ENTRY entry = new Winsvc.SERVICE_TABLE_ENTRY();
        Winsvc.SERVICE_TABLE_ENTRY[] table = (Winsvc.SERVICE_TABLE_ENTRY[]) entry.toArray(2);
        table[0].lpServiceName = SERVICE_NAME;
        table[0].lpServiceProc = serviceMainCallback;
        table[1].lpServiceName = null;
        table[1].lpServiceProc = null;

        if (!Advapi32.INSTANCE.StartServiceCtrlDispatcher(table)) {
            int error = Kernel32.INSTANCE.GetLastError();
            if (error == ERROR_FAILED_SERVICE_CONTROLLER_CONNECT) {
                McFlashLinkClientApplication.runApplication(serviceArgs(args));
                return;
            }
            throw new IllegalStateException("StartServiceCtrlDispatcher failed: " + error);
        }
    }

    private static void runServiceMain(String[] args) {
        serviceControlHandler = (control, eventType, eventData, context) -> {
            if (control == Winsvc.SERVICE_CONTROL_STOP || control == Winsvc.SERVICE_CONTROL_SHUTDOWN) {
                stopService();
                return WinError.NO_ERROR;
            }
            return WinError.NO_ERROR;
        };

        serviceStatusHandle = Advapi32.INSTANCE.RegisterServiceCtrlHandlerEx(SERVICE_NAME, serviceControlHandler, Pointer.NULL);
        if (serviceStatusHandle == null) {
            return;
        }

        reportStatus(Winsvc.SERVICE_START_PENDING, 0, 30000, 1);
        try {
            applicationContext = org.springframework.boot.SpringApplication.run(McFlashLinkClientApplication.class, serviceArgs(args));
            reportStatus(Winsvc.SERVICE_RUNNING, Winsvc.SERVICE_ACCEPT_STOP | Winsvc.SERVICE_ACCEPT_SHUTDOWN, 0, 0);
            stopLatch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            reportStatus(Winsvc.SERVICE_STOP_PENDING, 0, 10000, 2);
        } catch (RuntimeException exception) {
            reportStatus(Winsvc.SERVICE_STOPPED, 0, 0, 0);
            throw exception;
        } finally {
            closeApplication();
            reportStatus(Winsvc.SERVICE_STOPPED, 0, 0, 0);
        }
    }

    private static void stopService() {
        reportStatus(Winsvc.SERVICE_STOP_PENDING, 0, 30000, 1);
        closeApplication();
        CountDownLatch latch = stopLatch;
        if (latch != null) {
            latch.countDown();
        }
    }

    private static void closeApplication() {
        ConfigurableApplicationContext context = applicationContext;
        applicationContext = null;
        if (context != null) {
            context.close();
        }
    }

    private static void reportStatus(int currentState, int controlsAccepted, int waitHint, int checkPoint) {
        Winsvc.SERVICE_STATUS_HANDLE handle = serviceStatusHandle;
        if (handle == null) {
            return;
        }
        Winsvc.SERVICE_STATUS status = new Winsvc.SERVICE_STATUS();
        status.dwServiceType = WinNT.SERVICE_WIN32_OWN_PROCESS;
        status.dwCurrentState = currentState;
        status.dwControlsAccepted = controlsAccepted;
        status.dwWin32ExitCode = WinError.NO_ERROR;
        status.dwServiceSpecificExitCode = 0;
        status.dwCheckPoint = checkPoint;
        status.dwWaitHint = waitHint;
        Advapi32.INSTANCE.SetServiceStatus(handle, status);
    }

    private static String[] serviceArgs(String[] args) {
        List<String> result = new ArrayList<>();
        if (args != null) {
            for (String arg : args) {
                result.add(arg);
            }
        }
        if (!containsSpringProperty(result, "mc-flash-link.client.open-browser")) {
            result.add("--mc-flash-link.client.open-browser=false");
        }
        return result.toArray(new String[0]);
    }

    private static boolean containsSpringProperty(List<String> args, String propertyName) {
        String prefix = "--" + propertyName + "=";
        for (String arg : args) {
            if (arg != null && arg.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("windows");
    }
}
