package cn.com.mcflashlink.client.service.wintun;

public class WintunException extends RuntimeException {
    public WintunException(String message) {
        super(message);
    }

    public WintunException(String message, Throwable cause) {
        super(message, cause);
    }
}
