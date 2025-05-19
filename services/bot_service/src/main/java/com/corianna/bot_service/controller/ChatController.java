package com.corianna.bot_service.controller;

import java.util.UUID;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.corianna.bot_service.dto.ApiError;
import com.corianna.bot_service.dto.ApiResponse;
import com.corianna.bot_service.dto.Chatbot;
import com.corianna.bot_service.dto.KafkaMessageDTO;
import com.corianna.bot_service.dto.QuestionRequest;
import com.corianna.bot_service.dto.RequestIdResponse;
import com.corianna.bot_service.records.AnswerResponse;
import com.corianna.bot_service.service.AppService;
import com.corianna.bot_service.utils.MessageProducer;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/chat/{apikey}")
public class ChatController {

    private final AppService appService;
    private final MessageProducer messageProducer;
    private final RedisTemplate<String, AnswerResponse> redisTemplate;

    public ChatController(
            AppService appService,
            MessageProducer messageProducer,
            RedisTemplate<String, AnswerResponse> redisTemplate) {
        this.appService = appService;
        this.messageProducer = messageProducer;
        this.redisTemplate = redisTemplate;
    }

    @GetMapping("/info")
    public ResponseEntity<?> getBotInfo(@PathVariable("apikey") String apikey) {
        try {
            Chatbot chatbot = appService.getChatbotInfoByApiKey(apikey);

            if (chatbot == null) {
                return ResponseEntity.badRequest().body(
                        new ApiError(400, "Invalid API Key"));
            }

            return ResponseEntity.ok().body(
                    new ApiResponse<>(200, chatbot));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    new ApiError(500, "Internal Server Error"));
        }
    }

    @PostMapping("/question")
    public ResponseEntity<?> getQuestion(@RequestBody QuestionRequest data,
            @PathVariable("apikey") String apikey) {
        try {
            Chatbot chatbot = appService.getChatbotInfoByApiKey(apikey);

            if (chatbot == null) {
                return ResponseEntity.badRequest().body(
                        new ApiError(400, "Invalid API Key"));
            }

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
                    chatbot.getApp().getId(),
                    data.getChatId(),
                    data.getQuestion()));

            return ResponseEntity.ok().body(
                    new ApiResponse<>(200,
                            new RequestIdResponse(requestId)));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    new ApiError(500, "Internal Server Error"));
        }
    }

    @GetMapping("/answer")
    public ResponseEntity<?> getAnswer(@RequestParam String requestId) {
        try {

            AnswerResponse answerResponse = redisTemplate.opsForValue().get("answer::" + requestId);

            if (answerResponse == null) {
                return ResponseEntity.badRequest().body(
                        new ApiError(400, "Request either expired or not found or is generating"));
            }

            redisTemplate.delete(requestId);

            return ResponseEntity.ok().body(
                    new ApiResponse<>(200, answerResponse));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    new ApiError(500, "Internal Server Error"));
        }

    }

}
