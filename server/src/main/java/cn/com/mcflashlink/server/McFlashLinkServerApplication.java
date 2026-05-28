package cn.com.mcflashlink.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class McFlashLinkServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(McFlashLinkServerApplication.class, args);
    }
}
