package com.horrorpond.ingestion.application;

/**
 * itch.io 게임 페이지가 아니거나(목록·작성자 페이지 등) 모양이 바뀌어 필요한 값을 못 읽었다.
 */
public class ItchPageParseException extends RuntimeException {

    public ItchPageParseException(String message) {
        super(message);
    }
}
