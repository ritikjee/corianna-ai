package com.corianna.app_service.controllers;

import java.util.List;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.corianna.app_service.entity.IngestionAPI;
import com.corianna.app_service.services.IngestionAPIService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class IngestionAPIController {

    private final IngestionAPIService ingestionAPIService;
    private final HttpServletRequest request;

    public IngestionAPIController(IngestionAPIService ingestionAPIService, HttpServletRequest request) {
        this.ingestionAPIService = ingestionAPIService;
        this.request = request;
    }

    @QueryMapping
    public IngestionAPI getIngestionAPI(@Argument("appId") String appId, @Argument("ingestionId") String ingestionId) {
        String userId = request.getHeader("userId");
        if (userId == null) {
            throw new IllegalArgumentException("User ID is missing");
        }
        return ingestionAPIService.getIngestionAPI(userId, appId, ingestionId);
    }

    @QueryMapping
    public List<IngestionAPI> getIngestionAPIs(@Argument("appId") String appId) {
        String userId = request.getHeader("userId");
        if (userId == null) {
            throw new IllegalArgumentException("User ID is missing");
        }
        return ingestionAPIService.getIngestionAPIs(userId, appId);
    }

    @MutationMapping
    public IngestionAPI createIngestionAPI(@Argument("appId") String appId, @Argument("name") String name) {
        String userId = request.getHeader("userId");
        if (userId == null) {
            throw new IllegalArgumentException("User ID is missing");
        }
        return ingestionAPIService.createIngestionAPI(userId, appId, name);
    }

    @MutationMapping
    public String deleteIngestionAPI(@Argument("appId") String appId,
            @Argument("ingestionAPIId") String ingestionAPIId) {
        String userId = request.getHeader("userId");
        if (userId == null) {
            throw new IllegalArgumentException("User ID is missing");
        }
        return ingestionAPIService.deleteIngestionAPI(userId, appId, ingestionAPIId);
    }

    @MutationMapping
    public IngestionAPI updateIngestionAPIActiveStatus(
            @Argument("appId") String appId,
            @Argument("ingestionAPIId") String ingestionAPIId,
            @Argument("active") Boolean isActive) {
        String userId = request.getHeader("userId");
        if (userId == null) {
            throw new IllegalArgumentException("User ID is missing");
        }
        return ingestionAPIService.updateIngestionAPIActiveStatus(userId, appId, ingestionAPIId, isActive);
    }
}
