package cn.com.mcflashlink.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.awt.Desktop;
import java.net.URI;

@SpringBootApplication
public class McFlashLinkClientApplication {
    private static final String LOCAL_CONSOLE_URL = "http://127.0.0.1:26333";

    @Value("${mc-flash-link.client.open-browser:true}")
    private boolean openBrowser;

    public static void main(String[] args) {
        SpringApplication.run(McFlashLinkClientApplication.class, args);
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
