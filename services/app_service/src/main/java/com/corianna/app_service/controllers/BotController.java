package com.corianna.app_service.controllers;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.corianna.app_service.entity.Chatbot;
import com.corianna.app_service.services.ChatbotService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class BotController {

    private final ChatbotService chatbotService;
    private final HttpServletRequest request;

    public BotController(ChatbotService chatbotService, HttpServletRequest request) {
        this.chatbotService = chatbotService;
        this.request = request;
    }

    @QueryMapping
    public Chatbot getChatbot(@Argument("appId") String appId) {
        String userId = (String) request.getAttribute("userId");
        if (userId == null) {
            throw new IllegalArgumentException("User ID is missing in the request.");
        }
        return chatbotService.getChatbot(appId, userId);
    }

    @MutationMapping
    public Chatbot createChatbot(@Argument("appId") String appId) {
        String userId = (String) request.getAttribute("userId");
        if (userId == null) {
            throw new IllegalArgumentException("User ID is missing in the request.");
        }
        return chatbotService.createChatbot(appId, userId);
    }

    @MutationMapping
    public String deleteChatbot(@Argument("appId") String appId) {
        String userId = (String) request.getAttribute("userId");
        if (userId == null) {
            throw new IllegalArgumentException("User ID is missing in the request.");
        }
        return chatbotService.deleteChatbot(appId, userId);
    }

}
