package com.corianna.integration_service.dto;

public record SlackIntegrationInput(
        String appId) {

    public SlackIntegrationInput {
        if (appId == null || appId.isEmpty()) {
            throw new IllegalArgumentException("appId cannot be null or empty");
        }
    }
}
