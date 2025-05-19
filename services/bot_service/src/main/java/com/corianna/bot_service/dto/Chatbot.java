package com.corianna.bot_service.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Chatbot implements Serializable {

    private String id;
    private App app;
    private String apiKey;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
