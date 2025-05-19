package com.corianna.bot_service.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class App implements Serializable {

    private String id;
    private String name;
    private String url;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
