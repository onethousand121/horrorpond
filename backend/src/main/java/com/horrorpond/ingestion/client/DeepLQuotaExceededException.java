package com.horrorpond.ingestion.client;

/**
 * 456: 이달 무료 한도를 다 썼다. 다음 달에 이어서 번역한다.
 */
public class DeepLQuotaExceededException extends RuntimeException {

    public DeepLQuotaExceededException() {
        super("DeepL monthly character quota exceeded");
    }
}
