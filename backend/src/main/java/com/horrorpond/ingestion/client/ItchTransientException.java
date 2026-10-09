package com.horrorpond.ingestion.client;

/**
 * itch.io 5xx·429·I/O 오류처럼 다시 시도하면 성공할 수 있는 실패.
 */
public class ItchTransientException extends RuntimeException {

    public ItchTransientException(String message) {
        super(message);
    }

    public ItchTransientException(String message, Throwable cause) {
        super(message, cause);
    }
}
