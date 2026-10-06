package com.horrorpond.common.domain;

/**
 * 현재 상태에서 허용되지 않는 상태 전이.
 */
public class DomainStateException extends IllegalStateException {

    public DomainStateException(String message) {
        super(message);
    }
}
