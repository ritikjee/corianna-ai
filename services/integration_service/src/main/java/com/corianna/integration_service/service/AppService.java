package com.corianna.integration_service.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AppService {

    private final RestTemplate restTemplate;

    public AppService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
}
