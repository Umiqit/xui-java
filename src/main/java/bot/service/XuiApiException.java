package bot.service;

public class XuiApiException extends RuntimeException {
    public XuiApiException(String message) {
        super(message);
    }

    public XuiApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
