package com.horrorpond.ingestion.client;

/**
 * 5xx, I/O 오류처럼 다시 시도하면 성공할 수 있는 실패.
 */
public class SteamTransientException extends RuntimeException {

    public SteamTransientException(String message) {
        super(message);
    }

    public SteamTransientException(String message, Throwable cause) {
        super(message, cause);
    }
}
