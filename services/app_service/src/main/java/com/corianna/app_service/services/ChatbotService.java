package com.corianna.app_service.services;

import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Service;

import com.corianna.app_service.entity.Chatbot;
import com.corianna.app_service.entity.Member;
import com.corianna.app_service.enums.RoleEnum;
import com.corianna.app_service.repository.ChatbotRepository;
import com.corianna.app_service.repository.MembersRepository;

@Service
public class ChatbotService {

    private final MembersRepository membersRepository;
    private final ChatbotRepository chatbotRepository;

    public ChatbotService(MembersRepository membersRepository, ChatbotRepository chatbotRepository) {
        this.membersRepository = membersRepository;
        this.chatbotRepository = chatbotRepository;
    }

    @QueryMapping
    public Chatbot getChatbot(String appId, String userId) {
        membersRepository.findByUserIdAndAppId(userId, appId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Member not found for user ID: " + userId + " and app ID: " + appId));

        return chatbotRepository.findByAppId(appId)
                .orElseThrow(() -> new IllegalArgumentException("Chatbot not found for app ID: " + appId));
    }

    @MutationMapping
    public Chatbot createChatbot(String appId, String userId) {
        Member member = membersRepository.findByUserIdAndAppId(userId, appId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Member not found for user ID: " + userId + " and app ID: " + appId));

        if (member.getRole() != RoleEnum.OWNER) {
            throw new IllegalArgumentException("User does not have permission to create a chatbot for this app.");
        }

        Chatbot chatbot = new Chatbot();
        chatbot.setApp(member.getApp());

        return chatbotRepository.save(chatbot);

    }

    @MutationMapping
    public String deleteChatbot(String appId, String userId) {
        membersRepository.findByUserIdAndAppId(userId, appId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Member not found for user ID: " + userId + " and app ID: " + appId));

        Chatbot chatbot = chatbotRepository.findByAppId(appId)
                .orElseThrow(() -> new IllegalArgumentException("Chatbot not found for app ID: " + appId));

        chatbotRepository.delete(chatbot);

        return "Chatbot deleted successfully for App ID: " + appId;
    }
}
