package com.corianna.app_service.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.corianna.app_service.entity.Webhook;
import com.corianna.app_service.record.CreateWebhookInput;
import com.corianna.app_service.record.GraphQLWebhook;
import com.corianna.app_service.record.KeyValue;
import com.corianna.app_service.services.WebhookService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class WebhookController {

    private final WebhookService webhookService;
    private final HttpServletRequest request;

    public WebhookController(WebhookService webhookService, HttpServletRequest request) {
        this.webhookService = webhookService;
        this.request = request;
    }

    @QueryMapping
    public List<GraphQLWebhook> getWebhook(String appId) {

        String userId = request.getHeader("userId");

        if (userId == null) {
            throw new IllegalArgumentException("User ID is missing");
        }

        List<Webhook> webhook = webhookService.getWebhook(userId, appId);

        return webhook.stream()
                .map(w -> new GraphQLWebhook(w.getId(),
                        w.getName(),
                        w.getUrl(),
                        w.getToken(),
                        w.getHeaders().entrySet().stream()
                                .map(entry -> new KeyValue(entry.getKey(), entry.getValue()))
                                .toList(),
                        w.isActive(),
                        w.getCreatedAt().toString(),
                        w.getUpdatedAt().toString(),
                        w.getApp()))
                .toList();

    }

    @MutationMapping
    public GraphQLWebhook createWebhook(CreateWebhookInput input) {

        String userId = request.getHeader("userId");

        if (userId == null) {
            throw new IllegalArgumentException("User ID is missing");
        }

        Webhook webhook = webhookService.createWebhook(userId, input);

        Map<String, String> headers = webhook.getHeaders();
        List<KeyValue> headerList = headers.entrySet().stream()
                .map(entry -> new KeyValue(entry.getKey(), entry.getValue()))
                .toList();

        return new GraphQLWebhook(webhook.getId(),
                webhook.getName(),
                webhook.getUrl(),
                webhook.getToken(),
                headerList,
                webhook.isActive(),
                webhook.getCreatedAt().toString(),
                webhook.getUpdatedAt().toString(),
                webhook.getApp());

    }

    @MutationMapping
    public String deleteWebhook(String webhookId) {

        String userId = request.getHeader("userId");

        if (userId == null) {
            throw new IllegalArgumentException("User ID is missing");
        }

        return webhookService.deleteWebhook(userId, webhookId);
    }

    @MutationMapping
    public GraphQLWebhook updateWebhook(String webhookId, Boolean isActive) {

        String userId = request.getHeader("userId");

        if (userId == null) {
            throw new IllegalArgumentException("User ID is missing");
        }

        Webhook webhook = webhookService.updateWebhook(userId, webhookId, isActive);

        Map<String, String> headers = webhook.getHeaders();
        List<KeyValue> headerList = headers.entrySet().stream()
                .map(entry -> new KeyValue(entry.getKey(), entry.getValue()))
                .toList();

        return new GraphQLWebhook(webhook.getId(),
                webhook.getName(),
                webhook.getUrl(),
                webhook.getToken(),
                headerList,
                webhook.isActive(),
                webhook.getCreatedAt().toString(),
                webhook.getUpdatedAt().toString(),
                webhook.getApp());

    }

}
