package com.corianna.bot_service.records;

import java.time.LocalDateTime;

import com.corianna.bot_service.dto.App;

public record ChatbotResponse(
        String id,
        App app,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

}
