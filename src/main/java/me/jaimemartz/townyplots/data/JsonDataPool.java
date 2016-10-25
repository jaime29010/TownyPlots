package me.jaimemartz.townyplots.data;

import java.util.HashMap;
import java.util.Map;

public final class JsonDataPool {
    private final Map<String, JsonLocation> signs;

    public JsonDataPool() {
        signs = new HashMap<>();
    }

    public Map<String, JsonLocation> getSigns() {
        return signs;
    }
}
