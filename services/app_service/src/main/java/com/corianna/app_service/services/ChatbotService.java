package com.corianna.app_service.services;

import org.springframework.stereotype.Service;

import com.corianna.app_service.entity.Chatbot;
import com.corianna.app_service.entity.Member;
import com.corianna.app_service.enums.RoleEnum;
import com.corianna.app_service.repository.ChatbotRepository;

@Service
public class ChatbotService {

    private final MemberService memberService;
    private final ChatbotRepository chatbotRepository;

    public ChatbotService(MemberService memberService, ChatbotRepository chatbotRepository) {
        this.memberService = memberService;
        this.chatbotRepository = chatbotRepository;
    }

    public Chatbot getChatbot(String appId, String userId) {
        memberService.getMemberInfo(userId, appId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Member not found for user ID: " + userId + " and app ID: " + appId));

        return chatbotRepository.findByAppId(appId)
                .orElseThrow(() -> new IllegalArgumentException("Chatbot not found for app ID: " + appId));
    }

    public Chatbot createChatbot(String appId, String userId) {
        Member member = memberService.getMemberInfo(userId, appId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Member not found for user ID: " + userId + " and app ID: " + appId));

        if (member.getRole() != RoleEnum.OWNER) {
            throw new IllegalArgumentException("User does not have permission to create a chatbot for this app.");
        }

        Chatbot chatbot = new Chatbot();
        chatbot.setApp(member.getApp());

        return chatbotRepository.save(chatbot);

    }

    public String deleteChatbot(String appId, String userId) {
        memberService.getMemberInfo(userId, appId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Member not found for user ID: " + userId + " and app ID: " + appId));

        Chatbot chatbot = chatbotRepository.findByAppId(appId)
                .orElseThrow(() -> new IllegalArgumentException("Chatbot not found for app ID: " + appId));

        chatbotRepository.delete(chatbot);

        return "Chatbot deleted successfully for App ID: " + appId;
    }
}
