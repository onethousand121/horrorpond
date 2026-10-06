package com.horrorpond.common.domain;

/**
 * 도메인 규칙에 맞지 않는 입력값.
 */
public class DomainValidationException extends IllegalArgumentException {

    public DomainValidationException(String message) {
        super(message);
    }
}
