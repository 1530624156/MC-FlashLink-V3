package cn.com.mcflashlink.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
public class McFlashLinkClientApplication {
    private static final String LOCAL_CONSOLE_URL = "http://127.0.0.1:26333";
    private static final String SERVICE_ARG = "--service";

    @Value("${mc-flash-link.client.open-browser:true}")
    private boolean openBrowser;

    public static void main(String[] args) {
        if (hasArg(args, SERVICE_ARG)) {
            WindowsServiceRunner.run(withoutArg(args, SERVICE_ARG));
            return;
        }
        if (!isRunningAsAdmin()) {
            if (tryElevate(args)) {
                System.exit(0);
            }
        }
        SpringApplication.run(McFlashLinkClientApplication.class, args);
    }

    static void runApplication(String[] args) {
        SpringApplication.run(McFlashLinkClientApplication.class, args);
    }

    private static boolean hasArg(String[] args, String expectedArg) {
        if (args == null) {
            return false;
        }
        for (String arg : args) {
            if (expectedArg.equals(arg)) {
                return true;
            }
        }
        return false;
    }

    private static String[] withoutArg(String[] args, String unwantedArg) {
        if (args == null || args.length == 0) {
            return new String[0];
        }
        List<String> filteredArgs = new ArrayList<>();
        for (String arg : args) {
            if (!unwantedArg.equals(arg)) {
                filteredArgs.add(arg);
            }
        }
        return filteredArgs.toArray(new String[0]);
    }

    private static boolean isRunningAsAdmin() {
        try {
            if (!System.getProperty("os.name", "").toLowerCase().contains("windows")) {
                return true;
            }
            Process process = new ProcessBuilder("cmd.exe", "/c", "net session").redirectErrorStream(true).start();
            return process.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean tryElevate(String[] args) {
        try {
            List<String> command = new ArrayList<>();
            command.add("powershell.exe");
            command.add("-NoProfile");
            command.add("-Command");

            String javaPath = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java.exe";
            String jarPath = detectJarPath();
            String classPath = System.getProperty("java.class.path");

            String launchCmd;
            if (jarPath != null) {
                launchCmd = "& '" + javaPath + "' -jar '" + jarPath + "'";
            } else {
                launchCmd = "& '" + javaPath + "' -cp '" + classPath + "' " + McFlashLinkClientApplication.class.getName();
            }

            for (String arg : args) {
                launchCmd += " '" + arg.replace("'", "''") + "'";
            }

            command.add("Start-Process -FilePath 'powershell.exe' -ArgumentList '-NoProfile -Command " + launchCmd.replace("'", "''") + "' -Verb RunAs");

            new ProcessBuilder(command).start();
            return true;
        } catch (Exception e) {
            System.err.println("无法获取管理员权限，以普通权限继续运行: " + e.getMessage());
            return false;
        }
    }

    private static String detectJarPath() {
        try {
            String encodedPath = McFlashLinkClientApplication.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI().getPath();
            String path = URLDecoder.decode(encodedPath, StandardCharsets.UTF_8.name());
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            if (path.endsWith(".jar")) {
                return path;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void openBrowser() {
        if (!openBrowser) {
            return;
        }
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(LOCAL_CONSOLE_URL));
            }
        } catch (Exception ignored) {
        }
    }
}
