package com.horrorpond.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

public final class Fixtures {

    private Fixtures() {
    }

    /**
     * src/test/resources/fixtures/steam 아래 파일을 읽는다.
     */
    public static String steam(String name) {
        String path = "/fixtures/steam/" + name;
        try (InputStream in = Fixtures.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalArgumentException("fixture not found: " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static String appDetails(int appid) {
        return steam("appdetails-" + appid + ".json");
    }
}
