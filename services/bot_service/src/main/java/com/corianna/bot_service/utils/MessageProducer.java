package com.corianna.bot_service.utils;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.corianna.bot_service.dto.KafkaMessageDTO;

@Component
public class MessageProducer {

    private final KafkaTemplate<String, KafkaMessageDTO> kafkaTemplate;

    public MessageProducer(KafkaTemplate<String, KafkaMessageDTO> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessage(String topic, KafkaMessageDTO message) {
        kafkaTemplate.send(topic, message);
    }

}
