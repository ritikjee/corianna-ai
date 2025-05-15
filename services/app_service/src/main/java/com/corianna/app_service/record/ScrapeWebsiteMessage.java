package com.corianna.app_service.record;

import java.util.Map;

public record ScrapeWebsiteMessage(
                String url,
                String mode,
                String appId,
                Map<String, String> metadata) {

}
