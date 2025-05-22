package com.corianna.app_service.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.corianna.app_service.dto.ResponseDTO;
import com.corianna.app_service.entity.Chatbot;
import com.corianna.app_service.entity.Member;
import com.corianna.app_service.entity.Webhook;
import com.corianna.app_service.services.ChatbotService;
import com.corianna.app_service.services.MemberService;
import com.corianna.app_service.services.WebhookService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/internal-services")
public class InternalServicesController {

    private final WebhookService webhookService;
    private final ChatbotService chatbotService;
    private final MemberService memberService;

    public InternalServicesController(WebhookService webhookService, ChatbotService chatbotService,
            MemberService memberService) {
        this.webhookService = webhookService;
        this.chatbotService = chatbotService;
        this.memberService = memberService;
    }

    @GetMapping("/chatbot")
    public ResponseEntity<ResponseDTO<?>> getMethodName(@RequestParam String apiKey, HttpServletRequest request) {
        try {
            Chatbot chatbot = chatbotService.getChatbotByApiKey(apiKey);
            if (chatbot == null) {
                return ResponseEntity.badRequest().body(new ResponseDTO<>(
                        "Chatbot not found",
                        404,
                        "Chatbot not found for the provided API key",
                        null,
                        request.getRequestURI()));
            }
            return ResponseEntity.ok(new ResponseDTO<>(
                    "Chatbot found",
                    200,
                    null,
                    chatbot,
                    request.getRequestURI()));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ResponseDTO<>(
                    "Error occurred",
                    500,
                    e.getMessage(),
                    null,
                    request.getRequestURI()));
        }
    }

    @GetMapping("/webhooks")
    public ResponseEntity<ResponseDTO<?>> getWebhook(@RequestParam String appId, HttpServletRequest request) {
        try {
            List<Webhook> webhooks = webhookService.getWebhooks(appId);
            if (webhooks == null) {
                return ResponseEntity.badRequest().body(new ResponseDTO<>(
                        "Webhook not found",
                        404,
                        "Webhook not found for the provided API key",
                        null,
                        request.getRequestURI()));
            }
            return ResponseEntity.ok(new ResponseDTO<>(
                    "Webhook found",
                    200,
                    null,
                    webhooks,
                    request.getRequestURI()));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ResponseDTO<>(
                    "Error occurred",
                    500,
                    e.getMessage(),
                    null,
                    request.getRequestURI()));
        }
    }

    @GetMapping("/member")
    public ResponseEntity<ResponseDTO<?>> getMethodName(@RequestParam String userId, String appId,
            HttpServletRequest request) {
        System.out.println("User ID: " + userId);
        try {

            Member member = memberService.getMemberInfo(userId, appId).orElse(null);
            if (member == null) {
                return ResponseEntity.badRequest().body(new ResponseDTO<>(
                        "Member not found",
                        404,
                        "Member not found for the provided userId and appId",
                        null,
                        request.getRequestURI()));
            }

            return ResponseEntity.ok(new ResponseDTO<>(
                    "Member found",
                    200,
                    null,
                    member,
                    request.getRequestURI()));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ResponseDTO<>(
                    "Error occurred",
                    500,
                    e.getMessage(),
                    null,
                    request.getRequestURI()));
        }
    }

}
