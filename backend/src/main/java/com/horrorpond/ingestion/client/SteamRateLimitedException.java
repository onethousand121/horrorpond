package com.horrorpond.ingestion.client;

import lombok.Getter;

/**
 * Steam이 429/403으로 응답했다. 즉시 재시도하면 차단 위험이 있으므로 @Retryable 대상이 아니며,
 * 호출자가 명시적으로 대기한 뒤 다시 시도한다.
 */
@Getter
public class SteamRateLimitedException extends RuntimeException {

    private final int appid;
    private final int statusCode;

    public SteamRateLimitedException(int appid, int statusCode) {
        super("Steam rate limited: appid=" + appid + ", status=" + statusCode);
        this.appid = appid;
        this.statusCode = statusCode;
    }
}
