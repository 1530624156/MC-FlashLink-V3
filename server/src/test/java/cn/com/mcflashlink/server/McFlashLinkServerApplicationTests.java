package cn.com.mcflashlink.server;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "mc-flash-link.tunnel.udp-port=0")
class McFlashLinkServerApplicationTests {
    @Test
    void contextLoads() {
    }
}
