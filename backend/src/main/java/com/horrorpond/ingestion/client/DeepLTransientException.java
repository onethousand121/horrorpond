package com.horrorpond.ingestion.client;

/**
 * 429, 5xx, I/O 오류처럼 다시 시도하면 성공할 수 있는 DeepL 실패.
 */
public class DeepLTransientException extends RuntimeException {

    public DeepLTransientException(String message) {
        super(message);
    }

    public DeepLTransientException(String message, Throwable cause) {
        super(message, cause);
    }
}
