package com.corianna.app_service.utils;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.corianna.app_service.config.RabbitMQConfig;
import com.corianna.app_service.record.ScrapeWebsiteMessage;

@Service
public class MessageProducer {

    private final RabbitTemplate rabbitTemplate;

    public MessageProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendMessage(ScrapeWebsiteMessage message) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.WEBSITE_SCRAPE_QUEUE, message);
    }

}