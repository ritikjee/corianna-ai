package com.corianna.bot_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.corianna.bot_service.dto.Chatbot;
import com.corianna.bot_service.dto.ResponseDTO;

@Service
public class AppService {

    @Value("${app.service.url}")
    private String appServiceUrl;

    @Value("${secrets.x-app-secret}")
    private String xAppSecret;

    private final RestTemplate restTemplate;

    public AppService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Cacheable(value = "chatbot", key = "#apiKey")
    public Chatbot getChatbotInfoByApiKey(String apiKey) {

        try {

            String url = appServiceUrl + "/api/internal-services/chatbot?apiKey=" + apiKey;

            HttpHeaders headers = new HttpHeaders();
            headers.set("x-app-secret", xAppSecret);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<ResponseDTO<Chatbot>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<ResponseDTO<Chatbot>>() {
                    });

            if (response.getStatusCode().is2xxSuccessful()) {
                ResponseDTO<Chatbot> responseBody = response.getBody();
                if (responseBody != null && responseBody.getData() != null) {
                    return responseBody.getData();
                }
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

}
