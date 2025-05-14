package com.corianna.app_service.services;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.corianna.app_service.dto.ResponseDTO;
import com.corianna.app_service.dto.UserDTO;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class AuthService {

    @Value("${auth-service.url}")
    private String authServiceUrl;

    private final RestTemplate restTemplate;
    private final HttpServletRequest request;

    public AuthService(RestTemplate restTemplate, HttpServletRequest request) {
        this.restTemplate = restTemplate;
        this.request = request;
    }

    public UserDTO getUser() {

        HttpHeaders headers = new HttpHeaders();
        Collections.list(request.getHeaderNames())
                .forEach(headerName -> headers.set(headerName, request.getHeader(headerName)));

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<ResponseDTO<UserDTO>> responseEntity = restTemplate.exchange(
                    authServiceUrl + "/api/user/me",
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<ResponseDTO<UserDTO>>() {
                    });

            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                ResponseDTO<UserDTO> responseBody = responseEntity.getBody();
                if (responseBody != null) {
                    return responseBody.getData();
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

}
