package com.corianna.bot_service.dto;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class KafkaMessageDTO {

    private String requestId;
    private String appId;
    private String chatId;
    private String question;
    private Map<String, String> metadata;

}
