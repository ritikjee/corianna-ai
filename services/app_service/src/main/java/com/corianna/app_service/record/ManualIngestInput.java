package com.corianna.app_service.record;

import java.util.List;

public record ManualIngestInput(String appId, List<String> urls) {
    public ManualIngestInput {
        if (appId == null || appId.isEmpty()) {
            throw new IllegalArgumentException("App ID cannot be null or empty");
        }

        if (urls == null || urls.isEmpty()) {
            throw new IllegalArgumentException("URLs cannot be null or empty");
        }
        for (String url : urls) {
            if (url == null || url.isEmpty()) {
                urls.remove(url);
            }
        }
    }
}
