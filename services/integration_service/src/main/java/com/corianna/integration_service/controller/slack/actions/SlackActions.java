package com.corianna.integration_service.controller.slack.actions;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.corianna.integration_service.dto.SlackOAuthResponse;

@Component
public class SlackActions {

    @Value("${slack.client-id}")
    private String slackClientId;

    @Value("${slack.client-secret}")
    private String slackClientSecret;

    private final RestTemplate restTemplate;

    public SlackActions(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public SlackOAuthResponse getSlackOAuthResponseFromCode(String code) {
        try {

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<String> entity = new HttpEntity<>(
                    "client_id=" + slackClientId +
                            "&client_secret=" + slackClientSecret +
                            "&code=" + code,
                    headers);
            String url = "https://slack.com/api/oauth.v2.access";
            return restTemplate.postForObject(url, entity, SlackOAuthResponse.class);
        } catch (Exception e) {
            return null;
        }
    }

}
