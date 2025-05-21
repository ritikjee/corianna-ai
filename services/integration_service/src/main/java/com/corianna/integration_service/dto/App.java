package com.corianna.integration_service.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public record App(
        String id,
        String name,
        String url,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) implements Serializable {
}