package com.corianna.bot_service.dto;

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
    private String websiteId;
    private String question;

}
