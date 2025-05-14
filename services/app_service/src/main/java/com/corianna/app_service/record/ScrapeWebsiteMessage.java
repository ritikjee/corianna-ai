package com.corianna.app_service.record;

public record ScrapeWebsiteMessage(
        String url,
        String mode,
        String appId) {

}
