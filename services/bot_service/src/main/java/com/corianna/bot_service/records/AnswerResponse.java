package com.corianna.bot_service.records;

public record AnswerResponse(
        String question,
        String answer,
        String requestId,
        String appId,
        String chatId) {

}
