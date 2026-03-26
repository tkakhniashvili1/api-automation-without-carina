package com.solvd.util;

import java.util.UUID;

public final class TestDataFactory {

    private TestDataFactory() {
    }

    public static String uniqueName() {
        return "API User " + UUID.randomUUID().toString().substring(0, 8);
    }

    public static String uniqueEmail() {
        return "api." + UUID.randomUUID().toString().replace("-", "") + "@example.test";
    }
}
