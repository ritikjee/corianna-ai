package com.corianna.app_service.record;

import java.util.List;

public record CreateWebhookInput(
        String appId,
        String name,
        String url,
        String token,
        List<KeyValue> headers) {

}
