package com.corianna.app_service.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.corianna.app_service.entity.Chatbot;

@Repository
public interface ChatbotRepository extends JpaRepository<Chatbot, String> {

    Optional<Chatbot> findByAppId(String appId);

    Optional<Chatbot> findByApiKey(String apiKey);

}
