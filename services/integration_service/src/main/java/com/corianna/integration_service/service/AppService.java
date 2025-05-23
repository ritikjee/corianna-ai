package com.corianna.integration_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.corianna.integration_service.dto.Member;
import com.corianna.integration_service.dto.ResponseDTO;

@Service
public class AppService {

    @Value("${app-service.url}")
    private String appServiceUrl;

    @Value("${secrets.x-app-secret}")
    private String appSecret;

    private final RestTemplate restTemplate;

    public AppService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Member getMemberInfo(String userId, String appId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-app-secret", appSecret);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        String url = appServiceUrl + "/api/internal-services/member?userId=" + userId + "&appId=" + appId;

        try {

            ResponseEntity<ResponseDTO<Member>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET, entity,
                    new ParameterizedTypeReference<ResponseDTO<Member>>() {
                    });
            if (response.getStatusCode().is2xxSuccessful()) {
                ResponseDTO<Member> responseBody = response.getBody();
                if (responseBody != null && responseBody.getData() != null) {
                    return responseBody.getData();
                }
                return null;
            }

            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }
}
