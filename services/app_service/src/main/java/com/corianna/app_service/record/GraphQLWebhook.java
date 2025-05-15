package com.corianna.app_service.record;

import java.util.List;

import com.corianna.app_service.entity.App;

public record GraphQLWebhook(
        String id,
        String name,
        String url,
        String token,
        List<KeyValue> headers,
        boolean isActive,
        String createdAt,
        String updatedAt,
        App app) {

}
