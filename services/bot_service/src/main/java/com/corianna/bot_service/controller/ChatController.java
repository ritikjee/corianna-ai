package com.corianna.bot_service.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.corianna.bot_service.dto.ApiError;
import com.corianna.bot_service.dto.ApiResponse;
import com.corianna.bot_service.dto.KafkaMessageDTO;
import com.corianna.bot_service.dto.QuestionRequest;
import com.corianna.bot_service.dto.RequestIdResponse;
import com.corianna.bot_service.utils.MessageProducer;

@RestController
@RequestMapping("/api/chat/{appId}")
public class ChatController {

    private final MessageProducer messageProducer;

    public ChatController(MessageProducer messageProducer) {
        this.messageProducer = messageProducer;
    }

    @PostMapping("/question")
    public ResponseEntity<?> getQuestion(@RequestBody QuestionRequest data,
            @PathVariable("appId") String appId) {
        try {
            String requestId = UUID.randomUUID().toString();

            if (data.getChatId() == null || data.getChatId().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new ApiError(400, "Chat ID is required"));
            }

            if (data.getQuestion() == null || data.getQuestion().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new ApiError(400, "Question cannot be empty"));
            }

            System.out.println("RequestId: " + requestId);

            messageProducer.sendMessage("chat-questions", new KafkaMessageDTO(
                    requestId,
                    appId,
                    data.getChatId(),
                    data.getQuestion()));

            return ResponseEntity.ok().body(
                    new ApiResponse<>(200,
                            new RequestIdResponse(requestId)));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(
                    new ApiError(500, "Internal Server Error"));
        }
    }

    // TODO : Implement the getAnswer method using SSE
}
