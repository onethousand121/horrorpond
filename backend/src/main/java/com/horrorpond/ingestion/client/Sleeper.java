package com.horrorpond.ingestion.client;

import java.time.Duration;

/**
 * 대기 동작을 추상화해 테스트에서 실제로 잠들지 않고 대기 시간만 검증할 수 있게 한다.
 */
public interface Sleeper {

    void sleep(Duration duration);
}
